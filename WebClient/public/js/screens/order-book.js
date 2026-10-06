import { optional, side } from '../util/format.js';
import { sideStyle } from '../util/views.js';
import { Table } from '../util/table.js';

const ORDER_COLUMNS = [
    { title: 'Side', value: order => side(order.side), className: order => sideStyle(order.side) },
    { title: 'User', value: order => order.userName },
    { title: 'Shares', value: order => order.remainingShares, number: true },
    { title: 'Price', value: order => order.price, number: true }
];

// One outcome's order book (like OrderBookController in the JavaFX client): LAST / BID / ASK / MID / SPREAD and the
// orders waiting to be executed. Made twice (events are binary) from the same <template> in index.html.
export class OrderBookPanel {
    #title;
    #stats; // the elements that show a statistic: data-stat names its field in the server's answer (OrderBookDto)
    #ordersTable;

    constructor(container) {
        // A <template> is not shown; each panel gets its own copy of the template's content.
        container.append(document.getElementById('order-book-template').content.cloneNode(true));
        this.#title = container.querySelector('.book-title');
        this.#stats = [...container.querySelectorAll('[data-stat]')];
        this.#ordersTable = new Table(container.querySelector('.book-orders'), {
            columns: ORDER_COLUMNS,
            emptyText: 'No waiting orders'
        });
    }

    show(book) {
        this.#title.textContent = `Order book: ${book.outcomeTitle}`;
        for (const stat of this.#stats) {
            stat.textContent = optional(book[stat.dataset.stat]);
        }
        // Like a trading screen: from the highest price down, so the sells are above the buys and the best ask meets
        // the best bid in the middle. The sort is stable: orders with the same price keep their time order.
        const orders = [...book.sellOrders, ...book.buyOrders].sort((a, b) => b.price - a.price);
        this.#ordersTable.setRows(orders);
    }

    // Empties the book while another event's data is on its way.
    clear() {
        this.#title.textContent = 'Order book';
        for (const stat of this.#stats) {
            stat.textContent = optional(null);
        }
        this.#ordersTable.setRows([]);
    }
}
