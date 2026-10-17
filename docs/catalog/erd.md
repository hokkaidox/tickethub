# ERD Catalog

Таблицы Catalog: площадки, схемы залов, места, мероприятия и цены. Кто чем владеет между сервисами — в [architecture.md](../architecture.md), раздел «Места: кто чем владеет».

```mermaid
erDiagram
    venue ||--o{ layout : "схемы залов"
    layout ||--o{ segment : ""
    layout ||--|{ seat : ""
    segment ||--o{ seat : ""
    layout ||--o{ event : ""
    event ||--|{ event_price : ""
    segment ||--o{ event_price : ""

    venue {
        UUID id PK
        String name
        String address
    }
    layout {
        UUID id PK
        UUID venue_id FK
        String name "«Большой зал», «Большой зал, сцена выдвинута»"
        Integer seat_count "N, считается при создании"
    }
    segment {
        UUID id PK
        UUID layout_id FK
        String name "Партер, Балкон, VIP"
    }
    seat {
        UUID layout_id PK, FK
        Integer seat_no PK "0..N-1, сквозной технический номер"
        UUID segment_id FK
        String section "nullable, подпись на билете"
        String row "nullable"
        String number "номер на билете"
    }
    event {
        UUID id PK
        UUID layout_id FK
        String title
        String status "DRAFT, PUBLISHED, CANCELLED"
        timestamptz starts_at
        timestamptz ends_at
    }
    event_price {
        UUID event_id PK, FK
        UUID segment_id PK, FK
        Decimal price "numeric(12,2)"
        String currency "ISO 4217"
    }
    outbox {
        UUID id PK "UUIDv7, задаёт порядок публикации"
        UUID aggregate_id "id мероприятия, ключ партиции Kafka"
        String event_type "EventPublished, EventCancelled"
        jsonb payload
        timestamptz created_at
    }
```

## Сущности

| Таблица | Что хранит |
| --- | --- |
| `venue` | Площадка: название и адрес |
| `layout` | Схема зала площадки. Одна площадка может иметь несколько схем: разные залы или один зал в разных конфигурациях |
| `segment` | Ценовая зона схемы: «Партер», «Балкон», «VIP». Сегмент принадлежит схеме, цены у него нет |
| `seat` | Место схемы. `seat_no` — технический номер от 0 до N−1, его видят Reservation и Order. `section`, `row`, `number` — то, что напечатано на билете |
| `event` | Мероприятие на конкретной схеме |
| `event_price` | Цена сегмента на конкретном мероприятии. Один зал на разных мероприятиях продаётся по разным ценам |
| `outbox` | События для Kafka, записываются в одной транзакции с изменением мероприятия. Polling publisher удаляет строку после отправки |

Сектор и сегмент — разные понятия. Сектор — подпись на билете, сегмент — ценовая зона. Часто они совпадают, но первые ряды сектора могут стоить дороже остальных.

## Правила, которые проверяет код

- Схема зала неизменна, если на неё ссылается хотя бы одно опубликованное мероприятие. Изменённый зал — новая схема. Только поэтому `seat_no` можно передавать в другие сервисы: номера никогда не сдвигаются.
- Закрыть часть мест на мероприятие нельзя: для этого создаётся отдельная схема без этих мест.
- Мероприятие публикуется (`DRAFT → PUBLISHED`), только если у каждого сегмента его схемы есть цена в `event_price`.
- `segment_id` в `event_price` принадлежит той же схеме, что и мероприятие.
- Цену получает Order по паре `(event_id, seat_no)`: место → сегмент → цена мероприятия. Цены после публикации менять можно, в заказе фиксируется цена на момент его создания.
- `EventPublished` несёт `eventId` и `seatCount` = `layout.seat_count`.

## Что намеренно не моделируется

Цены по расписанию (ранние продажи по датам), скидки и промокоды, закрытие мест на одно мероприятие, организаторы и права на редактирование, пересчёт валют, зоны без рассадки.
