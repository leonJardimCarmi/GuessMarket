import { FeeType, Method, Side, Status } from '../dto.js';

// Turns the server's values into the text the screens show (like Format in the JavaFX client).
// One place for it, so every screen shows the same value the same way.

const NO_VALUE = '-';

// The server's value -> the text shown for it. [Status.ACTIVE] uses the constant's value ('ACTIVE') as the key.
const STATUS_TEXT = {
    [Status.NOT_STARTED]: 'Not started',
    [Status.ACTIVE]: 'Active',
    [Status.CLOSED]: 'Closed'
};
const METHOD_TEXT = {
    [Method.LMSR]: 'LMSR',
    [Method.ORDER_BOOK]: 'Order Book'
};
// A space that never breaks the line (character 160): in a narrow column, "on purchase" moves to the next line
// as one piece instead of being split.
const NO_BREAK_SPACE = String.fromCharCode(160);
const FEE_TYPE_TEXT = {
    [FeeType.AT_PURCHASE]: `on${NO_BREAK_SPACE}purchase`,
    [FeeType.AT_CLOSE]: `on${NO_BREAK_SPACE}close`
};
const SIDE_TEXT = {
    [Side.BUY]: 'Buy',
    [Side.SELL]: 'Sell'
};

// Always 2 digits after the point: money(12.5) -> "12.50"
export function money(amount) {
    return amount.toFixed(2);
}

// With a sign, for profit / loss: "+12.50" or "-3.00"
export function signed(amount) {
    const text = money(amount);
    return text.startsWith('-') ? text : `+${text}`;
}

// For values that may be missing, like order book statistics before the first trade.
// "== null" is true for both null and undefined: the server leaves empty values out of its answer.
export function optional(value) {
    return value == null ? NO_VALUE : money(value);
}

// "?? value": a value we have no text for is shown as it came
export function status(value) {
    return STATUS_TEXT[value] ?? value;
}

export function method(value) {
    return METHOD_TEXT[value] ?? value;
}

// e.g. "2.00% on purchase"
export function fee(event) {
    return `${money(event.feePercentage)}% ${FEE_TYPE_TEXT[event.feeType] ?? event.feeType}`;
}

export function side(value) {
    return SIDE_TEXT[value] ?? value;
}

// The time of day, 24-hour clock: "14:05:09"
export function time(date) {
    return date.toLocaleTimeString('en-GB');
}
