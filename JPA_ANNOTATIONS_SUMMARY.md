# JPA Annotations Implementation Summary

## Overview
Added JPA (Jakarta Persistence API) annotations to all entity classes for PostgreSQL database mapping. The application uses Spring Data JPA with Hibernate as the ORM provider.

---

## Entity Mapping Architecture

### Database Schema
```
┌─────────────────────────────────────┐
│           client                    │
├─────────────────────────────────────┤
│ id (UUID, PK)                       │
│ name                                │
│ email (unique)                      │
│ created_at                          │
│ updated_at                          │
│ portfolio_id (FK)                   │
└─────────────────────────────────────┘
        │
        └─────── OneToOne ────────────────┐
                                           │
                                           ▼
┌──────────────────────┐      ┌─────────────────────────┐
│    portfolio         │      │  portfolio_holdings     │
├──────────────────────┤      ├─────────────────────────┤
│ id (UUID, PK)        │      │ portfolio_id (FK)       │
│ total_value          │◄─────│ asset_ref (Asset)       │
└──────────────────────┘      │ quantity                │
                              │ avg_buy_price           │
                              └─────────────────────────┘
                              
┌──────────────────────────┐
│    client_watchlist      │
├──────────────────────────┤
│ client_id (FK)           │
│ asset_ref (Asset)        │
└──────────────────────────┘

┌──────────────────────────┐
│       orders             │
├──────────────────────────┤
│ order_id (UUID, PK)      │
│ client_id (UUID)         │
│ asset_ref (Asset)        │
│ quantity                 │
│ price                    │
│ buy (boolean)            │
│ order_date               │
└──────────────────────────┘
        │
        └─── ElementCollection ────────────────┐
                                               ▼
                                  ┌─────────────────────────┐
                                  │ order_status_log        │
                                  ├─────────────────────────┤
                                  │ order_id (FK)           │
                                  │ status (enum, key)      │
                                  │ status_change_time      │
                                  └─────────────────────────┘
```

---

## Classes Updated

### 1. **User.java** (Abstract Base Class)
- **Annotation**: `@MappedSuperclass`
- **Purpose**: Makes User a JPA mapped superclass so Client inherits its fields
- **Changes**:
  - Added JPA imports
  - Removed `final` from `id` field (JPA needs to set it)
  - Added `@Id` and `@GeneratedValue(strategy = GenerationType.UUID)`

```java
@MappedSuperclass
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### 2. **Client.java** (Entity)
- **Table**: `client`
- **Inheritance**: Inherits `id`, `name`, `email`, `createdAt`, `updatedAt` from User
- **Key Annotations**:
  - `@Entity`: Marks as JPA entity
  - `@Table(name = "client")`: Maps to client table
  - `@OneToOne`: Relationship to Portfolio (cascade delete when client deleted)
  - `@ManyToMany`: Relationship to Asset watchlist with join table

```java
@Entity
@Table(name = "client")
public class Client extends User {
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "client_watchlist",
        joinColumns = @JoinColumn(name = "client_id"),
        inverseJoinColumns = @JoinColumn(name = "asset_id")
    )
    private Set<Asset> watchlist;
}
```

**Features**:
- OneToOne Portfolio with `orphanRemoval = true` (portfolio deleted when client deleted)
- ManyToMany Asset watchlist with join table `client_watchlist`
- Lazy loading for performance
- No-arg constructor added for JPA

### 3. **Portfolio.java** (Entity)
- **Table**: `portfolio`
- **Key Annotations**:
  - `@Entity`: JPA entity
  - `@Table(name = "portfolio")`: Database table name
  - `@ElementCollection`: Collection of embedded Holding objects
  - `@CollectionTable`: Specifies portfolio_holdings table

```java
@Entity
@Table(name = "portfolio")
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "portfolio_holdings", 
                     joinColumns = @JoinColumn(name = "portfolio_id"))
    private Set<Holding> holdings;
    
    private BigDecimal totalValue;
}
```

**Features**:
- UUID primary key with auto-generation
- ElementCollection of embedded Holding objects (not a separate entity)
- Stored in `portfolio_holdings` table with portfolio_id foreign key
- Lazy loading for large portfolios
- Initialize `totalValue` to ZERO for proper calculations

### 4. **Holding.java** (Embeddable Value Object)
- **Database**: Embedded in `portfolio_holdings` collection table
- **Key Annotations**:
  - `@Embeddable`: Marks as an embeddable component (not a separate entity)
  - References to Asset (in-memory value object)
  - `@Column(precision=19, scale=8)`: For BigDecimal financial calculations

```java
@Embeddable
public class Holding {
    
    private Asset asset;  // References in-memory Asset object
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal avgBuyPrice;
}
```

**Features**:
- Embedded as part of Portfolio, not a separate entity table
- Stored in `portfolio_holdings` join table
- No separate `id` field (belongs to Portfolio)
- Precision=19, Scale=8 for accurate financial calculations
- Asset is an in-memory reference, not a database relationship

### 5. **Asset.java** (Value Object - NOT a JPA Entity)
- **Status**: Non-entity domain object
- **Purpose**: Represents a tradable asset (stock, bond, crypto, etc.)
- **Properties**:
  - `symbol`: Unique asset identifier (e.g., "AAPL", "BTC")
  - `name`: Human-readable asset name
  - `instrument`: Type of asset (CASH, EQUITY, BOND, CRYPTO)
  - `currMarketPrice()`: Mock service for current market price

```java
public class Asset {
    private final String symbol;
    private final String name;
    private final Instrument instrument;

    public Asset(String symbol, String name, Instrument instrument) {
        this.symbol = symbol;
        this.name = name;
        this.instrument = instrument;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public BigDecimal getCurrMarketPrice() {
        // Mock service
        BigDecimal price = new BigDecimal(2.0);
        return price;
    }
}
```

**Notes**:
- **Currently NOT mapped to a database table** - exists as an in-memory domain object
- Referenced by Holding entities (conceptually, not as a foreign key)
- Referenced by Client watchlist (conceptually)
- Referenced by Order entities (conceptually)
- **Future: Consider converting to an entity** if you need to:
  - Persist asset data to database
  - Track historical prices
  - Manage asset metadata (exchange, sector, etc.)
  - Query assets by properties

### 6. **Order.java** (Entity)
- **Table**: `orders` (reserved word, so plural)
- **Key Annotations**:
  - `@Entity`: JPA entity
  - `@ManyToOne`: Relationship to Asset
  - `@ElementCollection`: Status change log stored in separate table
  - `@CollectionTable`: Maps to order_status_log table
  - `@MapKeyEnumerated`: Status enum used as map key

```java
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(nullable = false)
    private boolean buy;
    
    @Column(nullable = false)
    private Instant orderDate;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_status_log", 
                     joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "status")
    @Column(name = "status_change_time")
    private Map<Status, Instant> statusChangeLog;
    
    public Status getCurrentStatus() {
        if (statusChangeLog == null || statusChangeLog.isEmpty()) {
            return Status.SUBMITTED;
        }
        return statusChangeLog.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Status.SUBMITTED);
    }
    
    public void setStatus(Status status) {
        if (statusChangeLog == null) {
            statusChangeLog = new HashMap<>();
        }
        this.statusChangeLog.put(status, Instant.now());
    }
}
```

**Features**:
- Status change log stored in separate `order_status_log` table with Map<Status, Instant>
- **No persistent `currentStatus` field** - status is derived dynamically from the statusChangeLog
- `getCurrentStatus()` finds the entry with the latest Instant timestamp and returns its Status key
- Precision=19, Scale=8 for financial data (price, quantity)
- clientId stored as UUID column (not a relationship - can reference deleted clients)
- Both constructors initialize statusChangeLog to prevent null pointer exceptions
- Lazy loading for the status change log collection

### 7. **ClientRepository.java** (Spring Data JPA)
**Changes**:
- Fixed generic type parameter from `<Client, Long>` to `<Client, UUID>`
- Added helper method `findByEmail(String email)`

```java
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByEmail(String email);
}
```

---

## Key JPA Concepts Used

### 1. **Cascade Types**
- `CascadeType.ALL`: All operations (persist, merge, remove, refresh, detach)
- `CascadeType.PERSIST`: Only persist operation cascades
- `CascadeType.MERGE`: Only merge operation cascades

### 2. **Fetch Types**
- `FetchType.LAZY`: Load entity only when accessed (default for collections)
- `FetchType.EAGER`: Load entity immediately with parent

### 3. **Orphan Removal**
- `orphanRemoval = true`: Child entity deleted when removed from collection
- Used for Client→Portfolio relationship only
- Not needed for Portfolio→Holding since Holding is embedded (not a separate entity)

### 4. **Relationship Types**
- **OneToOne**: Client ↔ Portfolio (one client has one portfolio)
- **ElementCollection**: Portfolio → Holding (portfolio contains collection of embedded holdings)
- **No database foreign keys to Asset** (Asset is an in-memory value object, not persisted)

### 5. **Enumerations**
- `@Enumerated(EnumType.STRING)`: Store enum as string value
- Used for Instrument and Status (enables SQL queries like WHERE status = 'SUBMITTED')

### 6. **Collections & Embeddables**
- `@Embeddable`: For non-entity value objects (like Holding)
- `@ElementCollection`: For collections of embeddables or scalars
- `@CollectionTable`: Specifies table for element collection (e.g., portfolio_holdings, order_status_log)

---

## Architecture Pattern: Status Tracking

The Order entity uses a **derived status pattern** rather than storing status as a persistent field:

- **Single Source of Truth**: `statusChangeLog` (Map<Status, Instant>) contains the complete order status history
- **Dynamic Derivation**: `getCurrentStatus()` finds the status with the latest timestamp
- **Benefits**:
  - Full audit trail preserved (when each status change occurred)
  - No need to maintain synchronization between status field and changelog
  - Queryable: Can find all statuses and their timestamps
  - Immutable changelog: Status changes are only added, never modified or deleted

---

## Next Steps
   ```java
   public interface ClientRepository extends JpaRepository<Client, UUID> {
       Optional<Client> findByEmail(String email);
       List<Client> findByNameContainingIgnoreCase(String name);
   }
   ```

2. **Create additional repositories**:
   - `OrderRepository` for Order queries
   - `PortfolioRepository` for Portfolio queries
   - *Note: Asset is NOT a JPA entity, so no repository needed*

3. **Database configuration** (application.properties):
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/eatrading
   spring.datasource.username=${DB_USERNAME}
   spring.datasource.password=${DB_PASSWORD}
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.show-sql=false
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
   ```

4. **Create service layer** to handle business logic above repositories

5. **Add validation annotations** (@NotNull, @Email, etc.) for data integrity

6. **Consider transaction management** with `@Transactional` on service methods

7. **Asset persistence consideration**:
   - Currently Asset is a value object (NOT persisted to database)
   - If you need to persist assets, convert it to a JPA `@Entity` with:
     - UUID primary key
     - Unique constraint on symbol column
     - Instrument enum stored as STRING
     - Then create `AssetRepository` for queries

---

## Notes

- All JPA entities use UUID primary keys for distributed systems scalability
- **4 main tables**: client, portfolio, orders, and 3 collection tables
- BigDecimal used with precision=19, scale=8 for financial calculations
- Lazy loading configured for performance (avoid N+1 queries)
- Enums stored as STRING type for readability and SQL queryability
- All foreign keys use `nullable = false` for referential integrity
- No-arg constructors added for JPA requirements
- **Holding is @Embeddable** (not a separate @Entity) - embedded in Portfolio via ElementCollection
- **Asset is NOT a JPA entity** - it's an in-memory value object referenced by Holding, Order, and Client
