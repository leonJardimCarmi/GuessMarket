import { element } from './views.js';
import { optional } from './format.js';

// A table drawn from column definitions (like TableView + Tables in the JavaFX client). A column is an object:
//   title      the header's text
//   value      row => the cell's value
//   number     true: shown with 2 decimals, right-aligned, sorted as a number
//   group      optional: a header above neighboring columns (e.g. an outcome's name above "Shares" and "Value")
//   className  optional: row => a style class for the cell (e.g. green "Buy" / red "Sell")
// Clicking a column's header sorts by it; clicking again sorts the other way; a third time returns to the
// server's order - like a JavaFX TableView.
export class Table {
    #head;
    #body;
    #columns = [];
    #headerCells = [];  // one per column, for the sort marks
    #rows = [];         // as given, in the server's order
    #shownRows = [];    // as drawn, after sorting
    #rowsJson = null;   // the rows drawn now, as JSON: the same data is not drawn again
    #emptyText;
    #sort = null;       // { index, ascending } - or null: the server's order
    #rowKey;            // row => its unique key (e.g. the event's name), for the selection
    #onSelect;          // called with the row the user chose; null: the rows cannot be chosen
    #selectedKey = null;

    constructor(table, { columns, emptyText = '', rowKey = null, onSelect = null }) {
        this.#head = table.createTHead();
        this.#body = table.createTBody();
        this.#emptyText = emptyText;
        this.#rowKey = rowKey;
        this.#onSelect = onSelect;
        if (onSelect) {
            table.classList.add('selectable');
            // One listener for the whole body ("event delegation"): it also works for rows drawn later.
            this.#body.addEventListener('click', event => this.#onClick(event));
            this.#body.addEventListener('keydown', event => this.#onKeyDown(event));
        }
        this.setColumns(columns);
    }

    // Also for tables whose columns depend on the data (the participants: a group of columns per outcome).
    setColumns(columns) {
        this.#columns = columns;
        this.#sort = null;
        this.#drawHead();
        this.#drawBody();
    }

    setRows(rows) {
        const json = JSON.stringify(rows);
        if (json !== this.#rowsJson) {
            this.#rowsJson = json;
            this.#rows = rows;
            this.#drawBody();
        }
    }

    // The text shown when there are no rows.
    setEmptyText(text) {
        if (text !== this.#emptyText) {
            this.#emptyText = text;
            if (this.#rows.length === 0) {
                this.#drawBody();
            }
        }
    }

    // Marks the row with this key as chosen (null: none). Only the marks change - the rows are not drawn again.
    setSelectedKey(key) {
        this.#selectedKey = key;
        if (this.#onSelect) {
            this.#shownRows.forEach((row, index) => {
                this.#body.rows[index].setAttribute('aria-selected', String(this.#rowKey(row) === key));
            });
        }
    }

    // --- Drawing ---

    // One header row; with groups, two: the groups' names above their columns, the columns' own titles below.
    #drawHead() {
        const grouped = this.#columns.some(column => column.group);
        const topRow = document.createElement('tr');
        const bottomRow = document.createElement('tr');
        this.#headerCells = this.#columns.map((column, index) => {
            const cell = this.#headerCell(column, index);
            if (!grouped) {
                topRow.append(cell);
            } else if (!column.group) {
                cell.rowSpan = 2; // not in a group: one cell as high as both header rows
                topRow.append(cell);
            } else {
                if (column.group !== this.#columns[index - 1]?.group) { // the first column of its group
                    const groupCell = element('th', 'group-header', column.group);
                    groupCell.colSpan = this.#groupSize(index);
                    topRow.append(groupCell);
                }
                bottomRow.append(cell);
            }
            return cell;
        });
        this.#head.replaceChildren(...(grouped ? [topRow, bottomRow] : [topRow]));
    }

    // How many neighboring columns, from this one on, belong to its group.
    #groupSize(index) {
        let size = 1;
        while (this.#columns[index + size]?.group === this.#columns[index].group) {
            size++;
        }
        return size;
    }

    // A header is a button: it can be clicked, and reached with the keyboard.
    #headerCell(column, index) {
        const cell = element('th', column.number ? 'number' : '');
        const button = element('button', 'sort-button', column.title);
        button.type = 'button';
        button.addEventListener('click', () => this.#sortBy(index));
        cell.append(button);
        return cell;
    }

    #drawBody() {
        const focusedKey = this.#focusedRowKey(); // after drawing, the keyboard focus returns to the same row
        this.#shownRows = this.#sortedRows();
        if (this.#shownRows.length === 0) {
            const cell = element('td', 'empty-cell', this.#emptyText);
            cell.colSpan = this.#columns.length;
            const row = document.createElement('tr');
            row.append(cell);
            this.#body.replaceChildren(row);
            return;
        }
        this.#body.replaceChildren(...this.#shownRows.map((row, index) => this.#rowElement(row, index)));
        if (focusedKey !== null) {
            const index = this.#shownRows.findIndex(row => this.#rowKey(row) === focusedKey);
            this.#body.rows[index]?.focus();
        }
    }

    #rowElement(row, index) {
        const tr = document.createElement('tr');
        tr.dataset.index = index; // the row's place in #shownRows (an HTML attribute: data-index="3")
        if (this.#onSelect) {
            tr.tabIndex = 0; // reachable with the Tab key
            tr.setAttribute('aria-selected', String(this.#rowKey(row) === this.#selectedKey));
        }
        for (const column of this.#columns) {
            const value = column.value(row);
            const cell = column.number
                ? element('td', 'number', optional(value))
                : element('td', '', value ?? '');
            const styleClass = column.className?.(row); // ?.() calls it only if the column has one
            if (styleClass) {
                cell.classList.add(styleClass);
            }
            tr.append(cell);
        }
        return tr;
    }

    // --- Sorting ---

    // First click: ascending; second: descending; third: back to the server's order.
    #sortBy(index) {
        if (this.#sort?.index !== index) {
            this.#sort = { index, ascending: true };
        } else if (this.#sort.ascending) {
            this.#sort = { index, ascending: false };
        } else {
            this.#sort = null;
        }
        this.#headerCells.forEach((cell, cellIndex) => {
            if (this.#sort?.index === cellIndex) {
                cell.setAttribute('aria-sort', this.#sort.ascending ? 'ascending' : 'descending');
            } else {
                cell.removeAttribute('aria-sort');
            }
        });
        this.#drawBody();
    }

    #sortedRows() {
        if (this.#sort === null) {
            return this.#rows;
        }
        const column = this.#columns[this.#sort.index];
        const direction = this.#sort.ascending ? 1 : -1;
        // sort() changes the array itself, so it sorts a copy. Rows with equal values keep their order.
        return [...this.#rows].sort((a, b) => direction * compare(column.value(a), column.value(b)));
    }

    // --- Choosing a row (mouse or keyboard) ---

    #onClick(event) {
        const tr = event.target.closest('tr[data-index]');
        if (tr) {
            this.#choose(tr);
        }
    }

    // Enter / Space choose the focused row; the arrows move to the next / previous row and choose it.
    #onKeyDown(event) {
        const tr = event.target.closest('tr[data-index]');
        let target = null;
        if (event.key === 'Enter' || event.key === ' ') {
            target = tr;
        } else if (event.key === 'ArrowDown') {
            target = tr?.nextElementSibling;
        } else if (event.key === 'ArrowUp') {
            target = tr?.previousElementSibling;
        }
        if (target) {
            event.preventDefault(); // otherwise the arrows and the space bar would also scroll
            target.focus();
            this.#choose(target);
        }
    }

    #choose(tr) {
        this.#onSelect(this.#shownRows[Number(tr.dataset.index)]);
    }

    #focusedRowKey() {
        const focused = document.activeElement;
        if (!this.#onSelect || !this.#body.contains(focused) || focused.dataset.index === undefined) {
            return null;
        }
        return this.#rowKey(this.#shownRows[Number(focused.dataset.index)]);
    }
}

// Numbers by size, text alphabetically (upper / lower case alike); missing values last.
function compare(a, b) {
    if (a == null || b == null) {
        return (a == null) - (b == null);
    }
    return typeof a === 'number' ? a - b : String(a).localeCompare(String(b), 'en', { sensitivity: 'base' });
}
