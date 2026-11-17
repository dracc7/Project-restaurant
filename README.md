# Restaurant Reservation and Queue Management System

CSC 481 group project. Java 17+, Spring Boot 4.1, Spring Data JPA, PostgreSQL (H2 for development).

This repo currently has the **data layer** (entities + repositories) finished and tested.
Everyone else builds their service, DTOs and controller on top of it.

---

## 1. Get it running in Eclipse (about 5 minutes)

1. Clone the repo (or unzip the folder).
2. Eclipse: **File → Import → Maven → Existing Maven Projects** → Browse to the
   `restaurant-reservation` folder → Finish.
   Eclipse now downloads Spring and the other libraries. The first time takes a few
   minutes (watch the progress bar at the bottom right).
3. Open `src/main/java/com/restaurant/reservation/RestaurantReservationApplication.java`
   → right-click → **Run As → Java Application**.
4. The Console should end with something like:
   `Sample data loaded: 9 tables, 3 reservations, 3 waitlisted parties.`
   and `Tomcat started on port 8080`.

**Run the tests:** right-click the `src/test/java` folder → **Run As → JUnit Test**.
All tests should be green.

**Troubleshooting**
- *Red X / "Java 17 required":* right-click project → Properties → Java Build Path →
  Libraries → pick a Java 17+ JRE. Also check Window → Preferences → Java → Installed JREs.
- *Dependencies didn't download:* right-click project → **Maven → Update Project…** →
  tick "Force Update" → OK.
- *"Could not find artifact spring-boot-starter-webmvc":* in `pom.xml`, change it to
  `spring-boot-starter-web`, then Maven → Update Project.
- *Port 8080 already in use:* another copy is running. Stop it with the red square
  in the Console.

---

## 2. Project layout (who owns what)

```
src/main/java/com/restaurant/reservation/
├── RestaurantReservationApplication.java   ← starts the app
├── model/        Ernesto  entities: DiningTable, Reservation, WaitlistEntry, Customer, enums, RestaurantRules
├── repository/   Ernesto  database access: TableRepository, ReservationRepository, WaitlistRepository
├── util/         Ernesto  ConfirmationCodeGenerator
├── config/       Ernesto  SampleDataLoader (test data on startup)
├── dto/          Kodah, Ali, Keira, Angela   request/response objects for the UI
├── service/      Kodah, Ali, Keira, Angela   business logic
└── controller/   Kodah, Ali, Keira           REST endpoints for the React UI
```

Each of `dto`, `service` and `controller` has a `package-info.java` describing what goes there.

---

## 3. How the layers fit together

```
React UI  ──JSON──►  Controller  ──►  Service  ──►  Repository  ──►  Database
                     (gets a DTO)     (rules,       (Ernesto's      (H2 or
                                       builds        interfaces)     PostgreSQL)
                                       entities)
```

- **DTO** = carries data from/to the UI. Never saved.
- **Entity** (`Reservation`, `DiningTable`, `WaitlistEntry`) = what is stored.
- **Repository** = the ONLY thing that talks to the database. Services never write SQL.

---

## 4. Using the repositories in your service

Ask for the repository in your constructor. Spring passes it in automatically:

```java
@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final TableRepository tables;

    public ReservationService(ReservationRepository reservations, TableRepository tables) {
        this.reservations = reservations;
        this.tables = tables;
    }

    @Transactional   // all-or-nothing
    public Reservation book(ReservationRequest req) {
        // 1. DTO → entity
        Customer customer = new Customer(req.name(), req.phone(), req.email());
        String code = ConfirmationCodeGenerator.generateUnique(reservations::existsByConfirmationCode);
        Reservation r = new Reservation(customer, req.partySize(), req.startTime(),
                                        req.seatingPreference(), code);

        // 2. find a suitable table that's free for the whole slot (FR-2)
        DiningTable table = tables.findAllByOrderByTableNumberAsc().stream()
                .filter(t -> t.suits(r.getPartySize(), r.getSeatingPreference()))
                .filter(t -> reservations.isTableFree(t.getId(), r.getStartTime(), r.getEndTime(), null))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No table available at that time"));
        r.assignTable(table);

        // 3. save
        return reservations.save(r);
    }
}
```

### Methods you get

Every repository also has `save`, `findById`, `findAll`, `count`, `deleteById` built in.

| Repository | Method | Use it for |
|---|---|---|
| `TableRepository` | `findAllByOrderByTableNumberAsc()` | Staff floor view |
| | `findByStatusOrderByTableNumberAsc(status)` | e.g. all READY tables |
| | `findByTableNumber(n)` / `existsByTableNumber(n)` | Look up / check before adding |
| `ReservationRepository` | `findByConfirmationCodeIgnoreCase(code)` | Customer looks up a booking |
| | `existsByConfirmationCode(code)` | Used by `ConfirmationCodeGenerator.generateUnique` |
| | `findByDate(date)` | Staff "today's reservations" page |
| | `findActiveByTable(tableId)` | BOOKED/SEATED reservations at one table |
| | `isTableFree(tableId, start, end, ignoreId)` | **Double-booking check.** Pass the reservation's own id when modifying it, `null` for new ones |
| `WaitlistRepository` | `findActiveInOrder()` | The current line, first come first served |

### Model methods you'll use

- `DiningTable`: `canSeat(size)`, `suits(size, preference)`, `setStatus(...)`
- `Reservation`: `overlaps(...)`, `getEndTime()`, `assignTable(...)`, `cancel()`, `markSeated()`,
  `markCompleted()`, `markNoShow()`, `setPartySize/StartTime/SeatingPreference` (modify)
- `WaitlistEntry`: `fits(table)`, `hasPriority()`, `recordSkipped()`, `markNotified(now)`,
  `responseWindowExpired(now)`, `recordMissedResponse()`, `markSeated()`, `remove()`
- `Customer`: `matchesContact(phoneOrEmail)` (prove a booking is theirs)

### Team rules

1. **Change status through the methods** (`cancel()`, `markSeated()` …), never by setting fields.
   They refuse impossible changes and throw a clear error.
2. **Call `save(...)` after changing an entity** (outside `@Transactional` methods especially).
3. **Don't delete reservations or waitlist entries.** Cancel/remove them so we keep history.
4. **Time limits and counts live in `RestaurantRules`.** Never hard-code 90 minutes etc.
5. **Never return entities from controllers.** Copy into a response DTO.
6. **Put `@Transactional` on service methods that change several things** (e.g. cancel a
   reservation and update the table).

---

## 5. How the central database works

Tables are created automatically from the entity classes (`spring.jpa.hibernate.ddl-auto=update`).

**`dining_tables`**

| Column | Type | Notes |
|---|---|---|
| `id` | bigint | Primary key, auto-generated |
| `version` | bigint | Optimistic locking: stops two staff overwriting each other's change |
| `table_number` | int | Unique: the number on the floor |
| `capacity` | int | Seats |
| `area` | text | `INDOOR` / `OUTDOOR` |
| `status` | text | `READY` / `RESERVED` / `SEATED` / `UNAVAILABLE` |

**`reservations`**

| Column | Type | Notes |
|---|---|---|
| `id`, `version` | bigint | As above |
| `confirmation_code` | text | Unique, 6 characters, uppercase |
| `customer_name`, `customer_phone`, `customer_email` | text | From the embedded `Customer` |
| `party_size` | int | 1 to `MAX_PARTY_SIZE` |
| `start_time` | timestamp | Date and time |
| `seating_preference` | text | `INDOOR` / `OUTDOOR` / `NO_PREFERENCE` |
| `status` | text | `BOOKED` / `SEATED` / `COMPLETED` / `CANCELLED` / `NO_SHOW` |
| `table_id` | bigint | Foreign key → `dining_tables.id` (null until assigned) |
| `created_at` | timestamp | When it was booked |

**`waitlist_entries`**

| Column | Type | Notes |
|---|---|---|
| `id`, `version` | bigint | As above |
| `customer_name`, `customer_phone`, `customer_email` | text | From the embedded `Customer` |
| `party_size` | int | |
| `seating_preference` | text | |
| `joined_at` | timestamp | Sets the order of the line |
| `status` | text | `WAITING` / `NOTIFIED` / `SEATED` / `REMOVED` |
| `notified_at` | timestamp | When we last said "your table is ready" |
| `missed_responses`, `times_skipped` | int | Counters for removal and priority |

### Using PostgreSQL instead of H2

By default the app uses H2, a database inside the app that's wiped on every restart.
To use PostgreSQL:

1. Start PostgreSQL. Easiest with Docker: `docker compose up -d` in this folder.
   (Or install PostgreSQL and create database `restaurant`, user `restaurant`, password `restaurant`.)
2. Eclipse: Run → Run Configurations → your app → Arguments → Program arguments:
   `--spring.profiles.active=postgres`
3. Run. Settings are in `src/main/resources/application-postgres.properties`.

Data now survives restarts. Sample data only loads when the database is empty.

---

## 6. Open questions (placeholders in `RestaurantRules`)

- Reservation length (currently 90 minutes)
- Waitlist response window (10 minutes)
- Missed responses before removal (3)
- Skips before priority (3)
- Max online party size (12)
