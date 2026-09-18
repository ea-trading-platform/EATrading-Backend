package com.eatrading.backend;

import java.util.*;
import java.math.BigDecimal;

public class Trade {
    // private UUID tradeId; for db
    private UUID orderId;
    private Holding boughtHolding;
    private Holding soldHolding;
    // private BigDecimal marketPrice; calc from holding asset value and qty
}
