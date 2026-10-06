import * as api from '../api.js';
import { Method } from '../dto.js';
import { fee, method, money, status } from '../util/format.js';
import { showFacts } from '../util/views.js';
import { Table } from '../util/table.js';
import { OrderBookPanel } from './order-book.js';

const FROM_EVENT = '(event)'; // a trade without a seller: the shares came from the event itself (LMSR or mint)

// The tables' columns (see Table)
const EVENT_COLUMNS = [
    { title: 'Event', value: event => event.name },
    { title: 'Status', value: event => status(event.status) },
    { title: 'Method', value: event => method(event.tradingMethod) },
    { title: 'Fee', value: event => fee(event) },
    { title: 'Account', value: event => event.eventBalance, number: true }
];
const OUTCOME_COLUMNS = [
    { title: 'Outcome', value: outcome => outcome.title },
    { title: 'Current price', value: outcome => outcome.currentPrice, number: true },
    { title: 'Shares', value: outcome => outcome.sharesCount, number: true }
];
const HISTORY_COLUMNS = [
    { title: 'Buyer', value: trade => trade.buyerName },
    { title: 'Seller', value: trade => trade.sellerName ?? FROM_EVENT },
    { title: 'Outcome', value: trade => trade.outcomeTitle },
    { title: 'Shares', value: trade => trade.sharesBought, number: true },
    { title: 'Paid', value: trade => trade.amountPaid, number: true },
    { title: 'Fee', value: trade => trade.feePaid, number: true }
];

// The Events tab (like EventsController in the JavaFX client): every event on the server, of every method and status,
// filtered by method / status / fee type, and the selected event's details - facts, outcomes, order books
// (Order Book events), participants and trade history.
export class EventsTab {
    #context;
    #showError; // shows an error message (in the main screen's status bar)

    // Left: the filters and the events list
    #filters = [...document.querySelectorAll('#events-panel .filter')];
    #toggles = [...document.querySelectorAll('#events-panel .toggle')];
    #count = document.getElementById('events-count');
    #eventsTable;

    // Right: the selected event's details
    #hint = document.getElementById('events-hint');
    #details = document.getElementById('event-details');
    #eventName = document.getElementById('event-name');
    #description = document.getElementById('event-description');
    #facts = document.getElementById('event-facts');
    #outcomesTable;
    #orderBooks = document.getElementById('order-books');
    #firstBook;
    #secondBook;
    #participantsTable;
    #historyTable;

    #allEvents = [];
    #selectedName = null;     // kept by name: every refresh brings new objects for the same events
    #detailsEventName = null; // the event the details area was last filled for

    constructor(context, showError) {
        this.#context = context;
        this.#showError = showError;

        this.#eventsTable = new Table(document.getElementById('events-table'), {
            columns: EVENT_COLUMNS,
            rowKey: event => event.name,
            onSelect: event => this.#selectEvent(event)
        });
        this.#outcomesTable = new Table(document.getElementById('outcomes-table'), { columns: OUTCOME_COLUMNS });
        this.#firstBook = new OrderBookPanel(document.getElementById('first-book'));
        this.#secondBook = new OrderBookPanel(document.getElementById('second-book'));
        // Its columns depend on the selected event's outcomes: set in #showDetails
        this.#participantsTable = new Table(document.getElementById('participants-table'), {
            columns: [],
            emptyText: 'No participants yet'
        });
        this.#historyTable = new Table(document.getElementById('history-table'), {
            columns: HISTORY_COLUMNS,
            emptyText: 'No trades yet'
        });

        // A toggle button is on while aria-pressed="true"; a click turns it over and filters the list again.
        for (const toggle of this.#toggles) {
            toggle.addEventListener('click', () => {
                toggle.setAttribute('aria-pressed', String(toggle.getAttribute('aria-pressed') !== 'true'));
                this.#showList();
            });
        }
        document.getElementById('events-refresh').addEventListener('click', () => this.refresh());
    }

    // Back to the start for a new login: all filters on, nothing selected, no data from the last login.
    reset() {
        for (const toggle of this.#toggles) {
            toggle.setAttribute('aria-pressed', 'true');
        }
        this.#allEvents = [];
        this.#selectedName = null;
        this.#showList();
    }

    // Reloads now (the Refresh button, switching to this tab).
    refresh() {
        return this.#context.refreshNow(() => this.pull(), this.#showError);
    }

    // Fetches the events and the selected event's participants and order books, and returns the screen update.
    // Used by refresh() and by every round of the automatic refresh (see MainScreen).
    async pull() {
        const events = await api.getEvents();
        const selectedName = this.#selectedName;
        const selected = events.find(event => event.name === selectedName);
        const live = selected ? await fetchLiveDetails(selected) : null;
        return () => {
            this.#allEvents = events;
            this.#showList();
            if (live && selectedName === this.#selectedName) { // still the same event on screen
                this.#showLiveDetails(live);
            }
        };
    }

    // --- The list ---

    // Shows the events that pass the filters. The selected event stays selected if it is still shown, and its details
    // are shown with the fresh data; if it is no longer shown, the hint is shown instead.
    #showList() {
        const shown = this.#allEvents.filter(event => this.#passesFilters(event));
        this.#count.textContent = `Showing ${shown.length} of ${this.#allEvents.length} events`;
        this.#eventsTable.setEmptyText(this.#allEvents.length === 0
            ? 'No events yet. Events are added by uploading a file in the JavaFX client.'
            : 'No events match the filters.');
        this.#eventsTable.setRows(shown);

        const selected = shown.find(event => event.name === this.#selectedName) ?? null;
        this.#selectedName = selected?.name ?? null;
        this.#eventsTable.setSelectedKey(this.#selectedName);
        this.#showDetails(selected);
    }

    // A filter is a row of toggle buttons, one per value; an event passes if the toggle of its value is on.
    // The filter's data-field names the event's field it checks, the toggle's data-value a value (see index.html).
    #passesFilters(event) {
        return this.#filters.every(filter => {
            const toggle = filter.querySelector(`.toggle[data-value="${event[filter.dataset.field]}"]`);
            return toggle?.getAttribute('aria-pressed') === 'true';
        });
    }

    // The user chose an event (mouse or keyboard): show it at once, then load its participants and order books.
    #selectEvent(event) {
        if (event.name === this.#selectedName) {
            return;
        }
        this.#selectedName = event.name;
        this.#eventsTable.setSelectedKey(event.name);
        this.#showDetails(event);
        this.#loadLiveDetails(event);
    }

    // --- The details ---

    #showDetails(event) {
        this.#hint.hidden = event !== null;
        this.#details.hidden = event === null;
        if (event === null) {
            return;
        }
        const orderBook = event.tradingMethod === Method.ORDER_BOOK;

        if (event.name !== this.#detailsEventName) {
            // Another event than before: drop the previous event's participants and books before the new ones arrive.
            this.#participantsTable.setColumns(participantColumns(event.outcomes));
            this.#participantsTable.setRows([]);
            this.#firstBook.clear();
            this.#secondBook.clear();
            this.#detailsEventName = event.name;
        }

        this.#eventName.textContent = event.name;
        this.#description.textContent = event.description;
        showFacts(this.#facts, this.#factsOf(event, orderBook));
        this.#outcomesTable.setRows(event.outcomes);
        this.#historyTable.setRows([...event.transactions].reverse()); // newest first
        this.#orderBooks.hidden = !orderBook;
    }

    // [caption, value] pairs, for the boxes at the top of the details.
    #factsOf(event, orderBook) {
        const facts = [['Status', status(event.status)]];
        if (event.winningOutcome != null) {
            facts.push(['Winner', event.winningOutcome]);
        }
        const marketMaker = event.marketMakerName;
        facts.push(
            ['Method', method(event.tradingMethod)],
            ['Market maker', this.#context.isMe(marketMaker) ? `${marketMaker} (you)` : marketMaker],
            ['Fee', fee(event)],
            ['Event account', money(event.eventBalance)],
            ['Fees collected', money(event.totalFeesCollected)],
            orderBook ? ['Winning share pays (d)', money(event.d)] : ['Liquidity (b)', money(event.b)]);
        return facts;
    }

    async #loadLiveDetails(event) {
        try {
            const live = await fetchLiveDetails(event);
            if (event.name === this.#selectedName) { // the user may have chosen another event meanwhile
                this.#showLiveDetails(live);
            }
        } catch (error) {
            this.#context.handleError(error, this.#showError);
        }
    }

    #showLiveDetails(live) {
        this.#participantsTable.setRows(live.participants);
        if (live.firstBook) {
            this.#firstBook.show(live.firstBook);
            this.#secondBook.show(live.secondBook);
        }
    }
}

// The participants and the order books are not part of the event's data, so they come in separate requests. They are
// sent together (Promise.all waits for all of them), so together they take only as long as the slowest one.
// Events are binary: an Order Book event has exactly two books; an LMSR event has none (null).
async function fetchLiveDetails(event) {
    const orderBook = event.tradingMethod === Method.ORDER_BOOK;
    const [first, second] = event.outcomes;
    const [participants, firstBook, secondBook] = await Promise.all([
        api.getParticipants(event.name),
        orderBook ? api.getOrderBook(event.name, first.title) : null,
        orderBook ? api.getOrderBook(event.name, second.title) : null
    ]);
    return { participants, firstBook, secondBook };
}

// The columns depend on the event's outcomes: under each outcome's name, the shares held and their value.
function participantColumns(outcomes) {
    const columns = [{ title: 'Participant', value: p => p.marketMaker ? `${p.name} (MM)` : p.name }];
    for (const outcome of outcomes) {
        const title = outcome.title;
        columns.push(
            { title: 'Shares', group: title, value: p => p.holdings[title] ?? 0, number: true },
            { title: 'Value', group: title, value: p => p.holdingValues[title] ?? 0, number: true });
    }
    columns.push({ title: 'Total value', value: p => p.totalValue, number: true });
    return columns;
}
