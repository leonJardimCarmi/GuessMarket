import * as api from '../api.js';
import { Method, Side, Status } from '../dto.js';
import { method, money, side, signed, status } from '../util/format.js';
import { clearMessage, onSubmit, positiveAmount, showError, showFacts, showSuccess, sideStyle } from '../util/views.js';
import { Table } from '../util/table.js';

const PRICE_TICK = 0.01; // the highest order price is d - 0.01

const HOLDING_COLUMNS = [
    { title: 'Outcome', value: holding => holding.outcome },
    { title: 'Shares', value: holding => holding.shares, number: true },
    { title: 'Paid for them', value: holding => holding.paid, number: true }
];
const WAITING_ORDER_COLUMNS = [
    { title: 'Side', value: order => side(order.side), className: order => sideStyle(order.side) },
    { title: 'Outcome', value: order => order.outcomeTitle },
    { title: 'Shares left', value: order => order.remainingShares, number: true },
    { title: 'Price', value: order => order.price, number: true }
];

// One event from the logged-in user's side (like EventTradeController in the JavaFX client): their role, holdings,
// money, trades and waiting orders, and only the actions that fit - open / close (market maker), buy (LMSR) or
// place an order (Order Book) while the event is active.
export class EventTradeSection {
    #context;
    #showError;     // shows an error that belongs to no action (in the main screen's status bar)
    #onChanged;     // after an action: the whole account shows the new state
    #onEventChosen; // the "My events" table marks the chosen event

    #eventChoice = document.getElementById('trade-event');
    #noEventOption = this.#eventChoice.options[0]; // "Choose an event"
    #hint = document.getElementById('trade-hint');
    #details = document.getElementById('trade-details');
    #facts = document.getElementById('trade-facts');
    #actions = document.getElementById('trade-actions'); // a fieldset: disabling it disables every control inside
    #openAction = document.getElementById('open-action');
    #buyForm = document.getElementById('buy-form');
    #buyOutcome = document.getElementById('buy-outcome');
    #buyShares = document.getElementById('buy-shares');
    #orderForm = document.getElementById('order-form');
    #orderSide = document.getElementById('order-side');
    #orderOutcome = document.getElementById('order-outcome');
    #orderShares = document.getElementById('order-shares');
    #priceLabel = document.getElementById('order-price-label');
    #orderPrice = document.getElementById('order-price');
    #closeForm = document.getElementById('close-form');
    #closeWinner = document.getElementById('close-winner');
    #noActions = document.getElementById('no-actions');
    #actionMessage = document.getElementById('action-message');
    #waitingOrders = document.getElementById('waiting-orders');
    #holdingsTable;
    #waitingOrdersTable;
    #tradesTable;

    #events = [];       // all the events: for the chooser, and for d (the highest order price)
    #me = null;         // the user: for their role in the event
    #chosenName = null;

    constructor(context, { showError, onChanged, onEventChosen }) {
        this.#context = context;
        this.#showError = showError;
        this.#onChanged = onChanged;
        this.#onEventChosen = onEventChosen;

        this.#holdingsTable = new Table(document.getElementById('holdings-table'), { columns: HOLDING_COLUMNS });
        this.#waitingOrdersTable = new Table(document.getElementById('waiting-orders-table'), {
            columns: WAITING_ORDER_COLUMNS,
            emptyText: 'No waiting orders'
        });
        this.#tradesTable = new Table(document.getElementById('my-trades-table'), {
            columns: tradeColumns(context),
            emptyText: 'No trades yet'
        });

        this.#eventChoice.addEventListener('change', () => this.choose(this.#eventChoice.value || null));
        document.getElementById('open-button').addEventListener('click', () => this.#open());
        onSubmit(this.#buyForm, () => this.#buy());
        onSubmit(this.#orderForm, () => this.#placeOrder());
        onSubmit(this.#closeForm, () => this.#close());
    }

    get chosenEventName() {
        return this.#chosenName;
    }

    // Back to the start for a new login: no event chosen, empty fields, no data from the last login.
    reset() {
        this.#events = [];
        this.#me = null;
        this.#chosenName = null;
        this.#eventChoice.replaceChildren(this.#noEventOption);
        this.#eventChoice.value = '';
        for (const field of [this.#buyShares, this.#orderShares, this.#orderPrice]) {
            field.value = '';
        }
        this.#orderSide.value = Side.BUY;
        clearMessage(this.#actionMessage);
        // Emptied too: "Bought" / "Sold" depend on who is logged in, so the next user's rows must be drawn anew
        for (const table of [this.#holdingsTable, this.#waitingOrdersTable, this.#tradesTable]) {
            table.setRows([]);
        }
        this.#showDetails(null);
    }

    // Fetches the chosen event's details and returns the screen update. The update gets the events and the user,
    // which the Account tab fetches in the same round.
    async pull() {
        const eventName = this.#chosenName;
        const details = eventName === null ? null : await api.getMyEvent(eventName);
        return (events, me) => this.#update(events, me, details);
    }

    // Chooses an event: in the chooser, or from outside (a click in the "My events" table).
    choose(eventName) {
        if (eventName === this.#chosenName) {
            return;
        }
        this.#chosenName = eventName;
        this.#eventChoice.value = eventName ?? '';
        clearMessage(this.#actionMessage);
        this.#onEventChosen(eventName);
        this.#loadDetails(); // show the new choice at once, without waiting for the next refresh
    }

    #update(events, me, details) {
        this.#events = events;
        this.#me = me;
        this.#showChoices(events.map(event => event.name));
        if (details !== null && details.eventName === this.#chosenName) { // still the chosen event
            this.#showDetails(details);
        }
    }

    // The chooser lists all the events. It is rebuilt only when the list changed, and keeps the chosen event.
    #showChoices(names) {
        const shownNames = [...this.#eventChoice.options].slice(1).map(option => option.value);
        if (JSON.stringify(shownNames) !== JSON.stringify(names)) {
            // new Option(text, value): the text is shown as written, never as HTML
            this.#eventChoice.replaceChildren(this.#noEventOption, ...names.map(name => new Option(name, name)));
        }
        this.#eventChoice.value = this.#chosenName ?? '';
    }

    async #loadDetails() {
        const eventName = this.#chosenName;
        if (eventName === null) {
            this.#showDetails(null);
            return;
        }
        try {
            const details = await api.getMyEvent(eventName);
            if (eventName === this.#chosenName) { // the user may have chosen another event meanwhile
                this.#showDetails(details);
            }
        } catch (error) {
            this.#context.handleError(error, this.#showError);
        }
    }

    #showDetails(details) {
        this.#hint.hidden = details !== null;
        this.#details.hidden = details === null;
        if (details === null) {
            return;
        }
        const orderBook = details.tradingMethod === Method.ORDER_BOOK;

        showFacts(this.#facts, this.#factsOf(details));
        this.#showActions(details, orderBook);
        this.#holdingsTable.setRows(Object.entries(details.holdings).map(([outcome, shares]) =>
            ({ outcome, shares, paid: details.investedByOutcome[outcome] ?? 0 })));
        this.#waitingOrders.hidden = !orderBook;
        this.#waitingOrdersTable.setRows(details.openOrders);
        this.#tradesTable.setRows(details.trades); // the server sends them newest first
    }

    // [caption, value] pairs, for the boxes at the top.
    #factsOf(details) {
        const facts = [['Status', status(details.status)]];
        if (details.winningOutcome != null) {
            facts.push(['Winner', details.winningOutcome]);
        }
        facts.push(
            ['Method', method(details.tradingMethod)],
            ['My role', this.#roleIn(details)],
            ['Invested', money(details.invested)],
            ['Fees paid', money(details.feesPaid)],
            ['Received', money(details.received)]);
        if (details.marketMaker) {
            facts.push(['Commissions earned', money(details.commissionsEarned)]);
        }
        const closed = details.status === Status.CLOSED;
        facts.push([closed ? 'Profit / loss' : 'Profit / loss so far', signed(details.profitLoss)]);
        return facts;
    }

    #roleIn(details) {
        if (details.marketMaker) {
            return 'Market maker';
        }
        return this.#me?.participatedEvents.includes(details.eventName) ? 'Participant' : 'Not participating yet';
    }

    // Shows only the actions that fit the event's state and the user's role.
    #showActions(details, orderBook) {
        const active = details.status === Status.ACTIVE;
        const marketMaker = details.marketMaker;
        this.#openAction.hidden = !(marketMaker && details.status === Status.NOT_STARTED);
        this.#buyForm.hidden = !(active && !orderBook);
        this.#orderForm.hidden = !(active && orderBook);
        this.#closeForm.hidden = !(active && marketMaker);

        const noActions = noActionsText(details.status, marketMaker);
        this.#noActions.textContent = noActions ?? '';
        this.#noActions.hidden = noActions === null;

        const outcomes = Object.keys(details.holdings); // one entry per outcome, in their order
        for (const choice of [this.#buyOutcome, this.#orderOutcome, this.#closeWinner]) {
            setChoices(choice, outcomes);
        }
        const event = this.#events.find(e => e.name === details.eventName);
        this.#priceLabel.textContent = orderBook && event
            ? `Price (up to ${money(event.d - PRICE_TICK)}):`
            : 'Price:';
    }

    // --- Actions ---

    #open() {
        const eventName = this.#chosenName;
        this.#runAction(() => api.openEvent(eventName), `Event '${eventName}' is open for trading.`);
    }

    #buy() {
        const eventName = this.#chosenName;
        const outcome = this.#buyOutcome.value;
        let shares;
        try {
            shares = positiveAmount(this.#buyShares, 'Shares');
        } catch (error) {
            showError(this.#actionMessage, error.message);
            return;
        }
        this.#runAction(() => api.buy(eventName, outcome, shares), `Bought ${money(shares)} '${outcome}' shares.`);
    }

    #placeOrder() {
        const eventName = this.#chosenName;
        const orderSide = this.#orderSide.value;
        const outcome = this.#orderOutcome.value;
        let shares;
        let price;
        try {
            shares = positiveAmount(this.#orderShares, 'Shares');
            price = positiveAmount(this.#orderPrice, 'Price');
        } catch (error) {
            showError(this.#actionMessage, error.message);
            return;
        }
        this.#runAction(() => api.placeOrder(eventName, outcome, orderSide, price, shares),
            `${side(orderSide)} order placed: ${money(shares)} '${outcome}' shares at ${money(price)}. `
            + 'See below whether it was executed or is waiting.');
    }

    #close() {
        const eventName = this.#chosenName;
        const winner = this.#closeWinner.value;
        if (!winner) {
            showError(this.#actionMessage, 'Choose the winning outcome.');
            return;
        }
        // The browser's own OK / Cancel dialog: closing pays the winners and cannot be undone
        if (!window.confirm(`Close '${eventName}' with '${winner}' as the winner? `
            + 'The winners are paid now, and this cannot be undone.')) {
            return;
        }
        this.#runAction(() => api.closeEvent(eventName, winner),
            `Event '${eventName}' is closed. The winner is '${winner}'.`);
    }

    // Runs an action on the server. The actions are locked meanwhile (no double clicks); then the result is shown
    // next to them, and everything that may have changed is reloaded.
    async #runAction(action, successMessage) {
        this.#actions.disabled = true;
        clearMessage(this.#actionMessage);
        try {
            await action();
            showSuccess(this.#actionMessage, successMessage);
            this.#onChanged();
        } catch (error) {
            this.#context.handleError(error, message => showError(this.#actionMessage, message));
        } finally {
            this.#actions.disabled = false;
        }
    }
}

// "Bought" or "Sold" depends on who is logged in, so these columns ask the context.
function tradeColumns(context) {
    const boughtByMe = trade => context.isMe(trade.buyerName);
    return [
        { title: 'Action', value: trade => boughtByMe(trade) ? 'Bought' : 'Sold' },
        { title: 'Outcome', value: trade => trade.outcomeTitle },
        { title: 'Shares', value: trade => trade.sharesBought, number: true },
        // amountPaid is what the buyer paid, fee included; the fee goes to the market maker, not to the seller
        {
            title: 'Amount',
            value: trade => boughtByMe(trade) ? trade.amountPaid : trade.amountPaid - trade.feePaid,
            number: true
        },
        { title: 'Fee', value: trade => boughtByMe(trade) ? trade.feePaid : null, number: true }
    ];
}

function noActionsText(eventStatus, marketMaker) {
    if (eventStatus === Status.NOT_STARTED && !marketMaker) {
        return 'The event has not started yet: trading begins when its market maker opens it.';
    }
    if (eventStatus === Status.CLOSED) {
        return 'The event is closed: no more trading.';
    }
    return null;
}

// The details are shown again after every action and refresh: keep the user's choice when the options are the same.
function setChoices(select, options) {
    const shownOptions = [...select.options].map(option => option.value);
    if (JSON.stringify(shownOptions) !== JSON.stringify(options)) {
        select.replaceChildren(...options.map(option => new Option(option, option)));
    }
}
