import * as api from '../api.js';
import { money, time } from '../util/format.js';
import { EventsTab } from './events.js';
import { AccountTab } from './account.js';

// The main screen after login (like MainController in the JavaFX client): a header (user, balance, log out),
// the Events and Account tabs, and a status bar. While it is shown, it runs the automatic refresh.
export class MainScreen {
    #context;
    #screen = document.getElementById('main-screen');
    #userName = document.getElementById('header-user-name');
    #balance = document.getElementById('header-balance');
    #logoutButton = document.getElementById('logout-button');
    #tabs = [...document.querySelectorAll('#main-screen [role="tab"]')];
    #statusText = document.getElementById('status-text');
    #eventsTab;
    #accountTab;
    #tabControllers; // a tab button's id -> the controller of its panel
    #shownTab = null; // the controller of the tab on screen

    constructor(context) {
        this.#context = context;
        const showError = message => this.#showStatus(message, true);
        this.#eventsTab = new EventsTab(context, showError);
        // The Account tab fetches the user anyway, and shows their balance in the header itself.
        this.#accountTab = new AccountTab(context, showError, user => this.showUser(user));
        this.#tabControllers = new Map([['events-tab', this.#eventsTab], ['account-tab', this.#accountTab]]);

        this.#logoutButton.addEventListener('click', () => this.#logout());
        for (const tab of this.#tabs) {
            tab.addEventListener('click', () => this.#selectTab(tab));
        }
    }

    show(user) {
        this.#logoutButton.disabled = false;
        this.#eventsTab.reset();
        this.#accountTab.reset();
        this.#selectTab(this.#tabs[0]); // every login starts on the Events tab
        this.showUser(user);
        this.#showStatus(`Logged in as ${user.name}.`);
        this.#screen.hidden = false;
        this.#context.startPolling(() => this.#pullRound(), error => this.#onPollFailed(error));
    }

    hide() {
        this.#screen.hidden = true;
    }

    // The user's name and balance in the header, visible from every tab.
    showUser(user) {
        let balance = `Balance: ${money(user.balance)}`;
        if (user.reservedBalance > 0) { // money held for waiting buy orders
            balance += ` (available: ${money(user.balance - user.reservedBalance)})`;
        }
        this.#userName.textContent = user.name;
        this.#balance.textContent = balance;
    }

    // Shows the tab's panel and hides the others. The tab names its panel in aria-controls (see index.html);
    // aria-selected marks the chosen tab - for the CSS (its look) and for screen readers.
    // Switching to a tab shows fresh data at once; from then on the automatic refresh keeps it fresh.
    #selectTab(selectedTab) {
        for (const tab of this.#tabs) {
            const selected = tab === selectedTab;
            tab.setAttribute('aria-selected', String(selected));
            document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
        }
        this.#shownTab = this.#tabControllers.get(selectedTab.id);
        this.#shownTab.refresh();
    }

    // One round of the automatic refresh: first fetch (the page stays usable while waiting), then return the update.
    // Only the tab on screen is fetched. The Account tab fetches the user too and shows them in the header; with the
    // Events tab, the user is fetched alongside (Promise.all), for the header's balance.
    async #pullRound() {
        let showTab;
        if (this.#shownTab === this.#accountTab) {
            showTab = await this.#accountTab.pull();
        } else {
            const [showEvents, me] = await Promise.all([this.#eventsTab.pull(), api.getMe()]);
            showTab = () => {
                showEvents();
                this.showUser(me);
            };
        }
        return () => {
            showTab();
            this.#showStatus(`Up to date - last update ${time(new Date())}`);
        };
    }

    // A failed round: an ended session returns to the login screen (which stops the refresh);
    // anything else - e.g. the server is down - shows in the status bar, and the next rounds keep trying.
    #onPollFailed(error) {
        this.#context.handleError(error, message => this.#showStatus(message, true));
    }

    async #logout() {
        this.#logoutButton.disabled = true;
        await this.#context.logout();
    }

    #showStatus(message, isError = false) {
        this.#statusText.textContent = message;
        this.#statusText.classList.toggle('status-error', isError);
    }
}
