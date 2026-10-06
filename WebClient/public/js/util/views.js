import { AmountLimits, Side } from '../dto.js';

// Small helpers that several screens share (like Views in the JavaFX client).

// Digits with an optional decimal point: "10", "2.5", ".5" - no sign, no thousands separator, no "1e3".
const AMOUNT_TEXT = /^([0-9]+[.]?[0-9]*|[.][0-9]+)$/;

// A new element with an optional style class and text. The text is set with textContent, so it is shown exactly
// as written and never run as HTML - event and user names come from other users, so this matters.
export function element(tag, className = '', text = '') {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (text !== '') {
        node.textContent = text;
    }
    return node;
}

// Shows [caption, value] pairs as small boxes - the caption above, the value below.
export function showFacts(container, facts) {
    showData(container, facts, pairs => pairs.map(([caption, value]) => fact(caption, value)));
}

// The style class of an order's side: "Buy" in green, "Sell" in red.
export function sideStyle(orderSide) {
    return orderSide === Side.BUY ? 'side-buy' : 'side-sell';
}

function fact(caption, value) {
    const box = element('div', 'fact');
    box.append(element('span', 'fact-caption', caption), element('span', 'fact-value', value));
    return box;
}

const shownData = new WeakMap(); // container -> the data it shows now, as JSON

// Fills the container with the elements build(data) returns - but only when the data differs from what it shows.
// The automatic refresh brings the same data most of the time; drawing it again anyway would make the screen
// flicker, and would undo what the user is doing there (selecting text, the keyboard focus).
function showData(container, data, build) {
    const json = JSON.stringify(data);
    if (shownData.get(container) !== json) {
        shownData.set(container, json);
        container.replaceChildren(...build(data));
    }
}

// Runs action when the form is submitted - by its submit button, or by Enter in one of its fields.
// preventDefault() stops what the browser does by default with a form: loading a new page.
export function onSubmit(form, action) {
    form.addEventListener('submit', event => {
        event.preventDefault();
        action();
    });
}

// A result message next to the controls it belongs to: green for a success, red for an error.
export function showSuccess(messageElement, text) {
    showMessage(messageElement, text, 'status-ok');
}

export function showError(messageElement, text) {
    showMessage(messageElement, text, 'status-error');
}

export function clearMessage(messageElement) {
    messageElement.textContent = '';
    messageElement.hidden = true;
}

function showMessage(messageElement, text, styleClass) {
    messageElement.textContent = text;
    messageElement.classList.remove('status-ok', 'status-error');
    messageElement.classList.add(styleClass);
    messageElement.hidden = false;
}

// Reads an amount the user typed (money or shares) by the same rules the server applies (AmountLimits), so a mistake
// gets a clear message at once - like Views.positiveAmount in the JavaFX client.
// Throws an Error whose message can be shown to the user as is.
export function positiveAmount(input, fieldName) {
    const text = input.value.trim();
    const fraction = text.split('.')[1] ?? '';
    const decimals = fraction.replace(/0+$/, '').length; // zeros at the end do not count: "2.50" has 1 decimal place
    const value = Number(text);
    if (!AMOUNT_TEXT.test(text) || value <= 0 || decimals > AmountLimits.MAX_DECIMALS) {
        throw new Error(`${fieldName} must be a positive number with at most ${AmountLimits.MAX_DECIMALS} `
            + 'decimal places (for example 10 or 2.5).');
    }
    if (value > AmountLimits.MAX) {
        throw new Error(`${fieldName} can be at most ${AmountLimits.MAX.toLocaleString('en-US')}.`);
    }
    return value;
}
