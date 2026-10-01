package com.guessmarket.client.ui.account;

import com.guessmarket.client.ui.ClientContext;

/**
 * The Account tab: uploading event files, the other users, the user's own account (balance, deposit, ledger),
 * the events they take part in, and trading. Built in stage 5.6.
 */
public class AccountController {
    private ClientContext context;

    public void init(ClientContext context) {
        this.context = context;
    }
}
