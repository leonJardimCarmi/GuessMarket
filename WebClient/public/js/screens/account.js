import * as api from '../api.js';
import { method, money, status, time } from '../util/format.js';
import { clearMessage, onSubmit, positiveAmount, showError, showFacts, showSuccess } from '../util/views.js';
import { Table } from '../util/table.js';
import { EventTradeSection } from './event-trade.js';

const MY_EVENT_COLUMNS = [
    { title: 'Event', value: row => row.name },
    { title: 'My role', value: row => row.role },
    { title: 'Status', value: row => row.status },
    { title: 'Method', value: row => row.method },
    { title: 'My shares', value: row => row.shares }
];
const LEDGER_COLUMNS = [
    { title: '#', value: row => row.number },
    { title: 'Time', value: row => time(new Date(row.entry.timestamp)) },
    { title: 'Description', value: row => row.entry.description, className: () => 'long-text' },
    { title: 'Amount', value: row => row.entry.amount, number: true },
    { title: 'Balance after', value: row => row.entry.balanceAfter, number: true }
];

// The Account tab - the user screen (like AccountController in the JavaFX client, without uploading files, which is
// done in the JavaFX client): the other users, and the user's own account - balance and deposit, the events they
// take part in, one event's details and trading, and the account history.
export class AccountTab {
    #context;
    #showError;     // shows an error message (in the main screen's status bar)
    #onUserChanged; // the header shows the balance too

    #usersTable;
    #balanceFacts = document.getElementById('balance-facts');
    #depositForm = document.getElementById('deposit-form');
    #depositField = document.getElementById('deposit-amount');
    #depositButton = document.getElementById('deposit-button');
    #depositMessage = document.getElementById('deposit-message');
    #myEventsTable;
    #eventTrade;
    #ledgerTable;

    #ledgerRows = []; // newest first: { number, entry }
    #ledgerSize = 0;  // the account lines we already have: a refresh asks only for newer ones
    #login = 0;       // counts the logins: data fetched for an earlier login is not shown

    constructor(context, showError, onUserChanged) {
        this.#context = context;
        this.#showError = showError;
        this.#onUserChanged = onUserChanged;

        this.#usersTable = new Table(document.getElementById('users-table'), { columns: userColumns(context) });
        this.#myEventsTable = new Table(document.getElementById('my-events-table'), {
            columns: MY_EVENT_COLUMNS,
            emptyText: 'No events yet: upload a file in the JavaFX client to become a market maker, '
                + 'or choose an event below and trade in it.',
            rowKey: row => row.name,
            onSelect: row => this.#eventTrade.choose(row.name) // shows the event in "Event details and trading"
        });
        this.#eventTrade = new EventTradeSection(context, {
            showError,
            onChanged: () => this.refresh(),
            onEventChosen: eventName => this.#myEventsTable.setSelectedKey(eventName)
        });
        this.#ledgerTable = new Table(document.getElementById('ledger-table'), {
            columns: LEDGER_COLUMNS,
            emptyText: 'No account activity yet'
        });

        onSubmit(this.#depositForm, () => this.#deposit());
        document.getElementById('account-refresh').addEventListener('click', () => this.refresh());
    }

    // Back to the start for a new login: nothing of the last user is left on the tab.
    reset() {
        this.#login++;
        this.#ledgerRows = [];
        this.#ledgerSize = 0;
        this.#depositField.value = '';
        this.#setDepositing(false);
        clearMessage(this.#depositMessage);
        showFacts(this.#balanceFacts, []);
        // "(you)" depends on who is logged in, so the tables are emptied and the next user's rows drawn anew
        for (const table of [this.#usersTable, this.#myEventsTable, this.#ledgerTable]) {
            table.setRows([]);
        }
        this.#eventTrade.reset();
    }

    // Reloads now (the Refresh button, switching to this tab, after a deposit or an action).
    refresh() {
        return this.#context.refreshNow(() => this.pull(), this.#showError);
    }

    // Fetches the whole tab - the user, the users, the events, the new account lines and the chosen event's details -
    // all at once (Promise.all), and returns the screen update. Used by refresh() and by the automatic refresh.
    async pull() {
        const login = this.#login;
        const ledgerFrom = this.#ledgerSize;
        const [me, users, events, newEntries, showChosenEvent] = await Promise.all([
            api.getMe(),
            api.getUsers(),
            api.getEvents(),
            api.getAccountEntries(ledgerFrom),
            this.#eventTrade.pull()
        ]);
        return () => {
            if (login !== this.#login) {
                return; // fetched before the current login (e.g. the user logged out and in meanwhile)
            }
            this.#show(me, users, events, ledgerFrom, newEntries);
            showChosenEvent(events, me);
        };
    }

    #show(me, users, events, ledgerFrom, newEntries) {
        this.#onUserChanged(me);
        showFacts(this.#balanceFacts, [
            ['Balance', money(me.balance)],
            ['Reserved for buy orders', money(me.reservedBalance)],
            ['Available', money(me.balance - me.reservedBalance)]
        ]);
        this.#usersTable.setRows(users);
        this.#myEventsTable.setRows(myEventRows(me, events));
        this.#myEventsTable.setSelectedKey(this.#eventTrade.chosenEventName);

        // Two refreshes may run at once; only the one that asked from our current size adds its lines.
        if (ledgerFrom === this.#ledgerSize && newEntries.length > 0) {
            const newRows = newEntries.map((entry, index) => ({ number: ledgerFrom + index + 1, entry })); // from 1
            this.#ledgerRows = [...newRows.reverse(), ...this.#ledgerRows]; // newest first
            this.#ledgerSize += newEntries.length;
            this.#ledgerTable.setRows(this.#ledgerRows);
        }
    }

    async #deposit() {
        let amount;
        try {
            amount = positiveAmount(this.#depositField, 'The amount');
        } catch (error) {
            showError(this.#depositMessage, error.message);
            return;
        }
        this.#setDepositing(true);
        try {
            await api.deposit(amount);
            this.#depositField.value = '';
            showSuccess(this.#depositMessage, `Deposited ${money(amount)}.`);
            this.refresh();
        } catch (error) {
            this.#context.handleError(error, message => showError(this.#depositMessage, message));
        } finally {
            this.#setDepositing(false);
        }
    }

    // The field is locked too, not only the button: Enter in the field would send the same deposit again.
    #setDepositing(depositing) {
        this.#depositButton.disabled = depositing;
        this.#depositField.disabled = depositing;
    }
}

// "(you)" depends on who is logged in, so these columns ask the context.
function userColumns(context) {
    return [
        { title: 'User', value: user => context.isMe(user.name) ? `${user.name} (you)` : user.name },
        { title: 'Balance', value: user => user.balance, number: true },
        { title: 'Market maker', value: user => user.marketMaker ? 'Yes' : 'No' }
    ];
}

// The rows of "My events": the events the user is the market maker of, then the others they took part in -
// each event once (a Set keeps no duplicates).
function myEventRows(me, events) {
    const names = new Set([...me.marketMakerEvents, ...me.participatedEvents]);
    return [...names].map(name => {
        const event = events.find(e => e.name === name);
        return {
            name,
            role: me.marketMakerEvents.includes(name) ? 'Market maker' : 'Participant',
            status: event ? status(event.status) : '-',
            method: event ? method(event.tradingMethod) : '-',
            shares: sharesText(me.holdings[name])
        };
    });
}

// e.g. "Yes 10.00, No 2.50"; "-" when no shares are held
function sharesText(holdings) {
    const entries = Object.entries(holdings ?? {});
    if (entries.length === 0) {
        return '-';
    }
    return entries.map(([outcome, shares]) => `${outcome} ${money(shares)}`).join(', ');
}
