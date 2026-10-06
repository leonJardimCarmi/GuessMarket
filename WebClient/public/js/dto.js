// Fixed values that appear in the server's answers - the same as the constants in the Java DTO classes
// (MarketEventDto, OrderDto). Object.freeze works like Java's final: nobody can change them by mistake.

export const Status = Object.freeze({
    NOT_STARTED: 'NOT_STARTED',
    ACTIVE: 'ACTIVE',
    CLOSED: 'CLOSED'
});

export const Method = Object.freeze({
    LMSR: 'LMSR',
    ORDER_BOOK: 'ORDER_BOOK'
});

export const FeeType = Object.freeze({
    AT_PURCHASE: 'AT_PURCHASE',
    AT_CLOSE: 'AT_RESOLUTION'
});

export const Side = Object.freeze({
    BUY: 'BUY',
    SELL: 'SELL'
});

// The rules for amounts users type - money and shares - the same as AmountLimits in the Java DTO module:
// more than 0, at most 2 decimal places, at most MAX.
export const AmountLimits = Object.freeze({
    MAX_DECIMALS: 2,
    MAX: 1_000_000_000
});
