package com.rajarata.bank.interfaces;

import com.rajarata.bank.exceptions.InsufficientFundsException;
import com.rajarata.bank.exceptions.InvalidAccountException;

/**
 * Interface defining transactable behavior for any entity that can
 * perform financial transactions (deposits, withdrawals, transfers).
 * 
 * OOP Concept: Abstraction - This interface defines a contract that all
 * transactable entities must implement, hiding internal implementation details
 * while guaranteeing a consistent API for financial operations.
 * 
 * OOP Concept: Polymorphism - Different account types implement these methods
 * differently (e.g., SavingsAccount has withdrawal limits, CheckingAccount has overdraft).
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public interface Transactable {

    /**
     * Deposits the specified amount into the account.
     * 
     * @param amount The amount to deposit (must be >= $10)
     * @return true if deposit was successful
     * @throws InvalidAccountException if account is not in active status
     */
    boolean deposit(double amount) throws InvalidAccountException;

    /**
     * Withdraws the specified amount from the account.
     * 
     * @param amount The amount to withdraw
     * @return true if withdrawal was successful
     * @throws InsufficientFundsException if balance is insufficient
     * @throws InvalidAccountException if account is not in active status
     */
    boolean withdraw(double amount) throws InsufficientFundsException, InvalidAccountException;

    /**
     * Gets the current available balance for transactions.
     * This may differ from the total balance (e.g., overdraft availability).
     * 
     * @return The available balance for transactions
     */
    double getAvailableBalance();

    /**
     * Checks if the account can perform a withdrawal of the given amount.
     * 
     * @param amount The amount to check
     * @return true if the withdrawal is allowed
     */
    boolean canWithdraw(double amount);
}

