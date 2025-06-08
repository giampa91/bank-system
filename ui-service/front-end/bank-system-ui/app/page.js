'use client'; // This directive makes this component a Client Component

import React, { useState } from 'react';

// --- API Base URL ---
// In a real application, this would be an environment variable.
const API_BASE_URL = 'http://localhost:8080';

const API_BASE_URL_PAYMENT = 'http://localhost:8081';

// --- API Functions (using fetch to simulate Axios) ---
// These functions now make actual network requests to your backend.

/**
 * Fetches account details (user info, balance, transactions) for a given account number.
 * @param {string} accountNumber The account number to fetch details for.
 * @returns {Promise<Object>} A promise that resolves with the account data.
 */
const fetchAccountDetails = async (accountNumber) => {
  const response = await fetch(`${API_BASE_URL}/api/accounts/by-account-number/${accountNumber}`);

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'Failed to parse error response' }));
    throw new Error(errorData.message || `API error: ${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  // Assuming the API response structure directly provides accountNumber, accountHolderName, balance, and transactions.
  // We map accountNumber to id and accountHolderName to name for consistency with existing state.
  return {
    data: {
      id: data.accountNumber, // Mapping API's accountNumber to current 'id' state
      name: data.accountHolderName, // Mapping API's accountHolderName to current 'name' state
      balance: data.balance,
      transactions: data.transactions || [], // Ensure transactions is an array
    }
  };
};

/**
 * Sends a payment request to the backend.
 * @param {string} senderAccountId The ID of the sender's account.
 * @param {string} receiverAccountId The ID of the receiver's account.
 * @param {number} amount The amount to send.
 * @param {string} currency The currency of the payment (e.g., "Eur").
 * @param {string} idempotencyKey A unique key to prevent duplicate payments.
 * @returns {Promise<Object>} A promise that resolves with the payment confirmation.
 */
const sendPayment = async (senderAccountId, receiverAccountId, amount, currency, idempotencyKey) => {
  const response = await fetch(`${API_BASE_URL_PAYMENT}/api/payments/initiate`, { // Assuming /api/transactions is the payment endpoint
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      senderAccountId,
      receiverAccountId,
      amount,
      currency,
      idempotencyKey,
    }),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'Failed to parse error response' }));
    throw new Error(errorData.message || `Payment failed: ${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  return { data: { message: data.message || 'Payment successful!' } };
};

// Main Home component for app/page.js
export default function Home() {
  // State to manage the current view/page ('login', 'dashboard', 'payment')
  const [currentPage, setCurrentPage] = useState('login');
  // State to store authenticated user data
  const [currentUser, setCurrentUser] = useState(null);
  // State for login input field (now refers to accountNumber)
  const [accountNumberInput, setAccountNumberInput] = useState('');
  // State for loading indicator
  const [loading, setLoading] = useState(false);
  // State for displaying messages (success/error)
  const [message, setMessage] = useState('');

  // States for payment form inputs
  const [paymentRecipientId, setPaymentRecipientId] = useState('');
  const [paymentAmount, setPaymentAmount] = useState('');

  // --- Login Page Logic ---
  const handleLogin = async () => {
    setLoading(true); // Show loading indicator
    setMessage(''); // Clear previous messages
    try {
      // Fetch account details (user info, balance, transactions) from the backend
      const response = await fetchAccountDetails(accountNumberInput);
      const accountInfo = response.data;

      // Set current user with all their details from the API response
      setCurrentUser({
        id: accountInfo.id,
        name: accountInfo.name,
        balance: accountInfo.balance,
        transactions: accountInfo.transactions,
      });
      setCurrentPage('dashboard'); // Navigate to the dashboard
    } catch (error) {
      // Handle login errors
      setMessage(`Login failed: ${error.message}.`); // No longer suggest mock IDs
      setCurrentUser(null);
    } finally {
      setLoading(false); // Hide loading indicator
    }
  };

  // --- Payment Page Logic ---
  const handleMakePayment = async () => {
    setLoading(true); // Show loading indicator
    setMessage(''); // Clear previous messages
    try {
      const amount = parseFloat(paymentAmount);
      // Validate payment amount
      if (isNaN(amount) || amount <= 0) {
        setMessage('Please enter a valid positive amount.');
        setLoading(false);
        return;
      }
      if (!currentUser || !currentUser.id) {
          setMessage('Error: Sender account not found.');
          setLoading(false);
          return;
      }

      // Generate a random idempotency key for the payment
      const idempotencyKey = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

      // Send payment request to the backend
      const response = await sendPayment(
        currentUser.id, // Sender Account ID
        paymentRecipientId, // Receiver Account ID from input
        amount,
        'Eur', // Hardcoded currency as per curl example
        idempotencyKey
      );
      setMessage(response.data.message); // Display success message

      // Refresh dashboard data to reflect the new balance and transactions
      // This is crucial as payment changes the sender's balance and adds a transaction
      const dashboardResponse = await fetchAccountDetails(currentUser.id);
      setCurrentUser(prevUser => ({
        ...prevUser,
        balance: dashboardResponse.data.balance,
        transactions: dashboardResponse.data.transactions,
      }));
      // Clear payment form fields
      setPaymentRecipientId('');
      setPaymentAmount('');
    } catch (error) {
      // Handle payment errors
      setMessage(`Payment failed: ${error.message}`);
    } finally {
      setLoading(false); // Hide loading indicator
    }
  };

  // --- Logout Logic ---
  const handleLogout = () => {
    setCurrentUser(null); // Clear current user
    setAccountNumberInput(''); // Clear login input
    setMessage(''); // Clear messages
    setCurrentPage('login'); // Go back to login page
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center p-4 font-inter">
      <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-lg sm:max-w-xl text-center">
        {/* Loading Indicator Overlay */}
        {loading && (
          <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center rounded-xl z-10">
            <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-indigo-500"></div>
            <p className="ml-4 text-indigo-700 text-lg">Loading...</p>
          </div>
        )}

        {/* Global Message Display (for success/error) */}
        {message && (
          <div className={`mb-4 p-3 rounded-lg text-white ${message.includes('failed') || message.includes('error') ? 'bg-red-500' : 'bg-green-500'}`}>
            {message}
          </div>
        )}

        {/* --- Login Page UI --- */}
        {currentPage === 'login' && (
          <>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-indigo-800 mb-6">
              Welcome to Your Bank
            </h1>
            <p className="text-gray-600 mb-6">Enter your Account Number to login.</p>
            <input
              type="text"
              className="w-full p-3 mb-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition-all duration-200"
              placeholder="Enter Account Number (e.g., ACC-001-A)"
              value={accountNumberInput}
              onChange={(e) => setAccountNumberInput(e.target.value)}
              disabled={loading} // Disable input while loading
            />
            <button
              onClick={handleLogin}
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-indigo-300 disabled:opacity-50"
              disabled={loading || !accountNumberInput} // Disable button if loading or input is empty
            >
              {loading ? 'Logging In...' : 'Login'}
            </button>
          </>
        )}

        {/* --- Dashboard Page UI --- */}
        {currentPage === 'dashboard' && currentUser && (
          <>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-indigo-800 mb-6">
              Welcome, {currentUser.name}!
            </h1>
            <div className="mb-8 text-left">
              <p className="text-xl font-semibold text-gray-700 mb-2">Account Number:</p>
              <p className="text-2xl font-bold text-gray-800 mb-4">{currentUser.id}</p> {/* Display account number */}

              <p className="text-xl font-semibold text-gray-700 mb-2">Account Balance:</p>
              <p className="text-5xl font-bold text-green-600 mb-6">
                ${currentUser.balance.toFixed(2)}
              </p>

              <h2 className="text-2xl font-semibold text-indigo-700 mb-4">Recent Transactions</h2>
              {currentUser.transactions && currentUser.transactions.length > 0 ? (
                <ul className="divide-y divide-gray-200 bg-gray-50 p-4 rounded-lg">
                  {currentUser.transactions.map((transaction) => (
                    <li key={transaction.id} className="py-3 flex justify-between items-center">
                      <div>
                        <p className="font-medium text-gray-800">{transaction.description}</p>
                        <p className="text-sm text-gray-500">{transaction.date}</p>
                      </div>
                      <p className={`font-semibold text-lg ${transaction.type === 'debit' ? 'text-red-600' : 'text-green-600'}`}>
                        {transaction.type === 'debit' ? '-' : '+'}${Math.abs(transaction.amount).toFixed(2)}
                      </p>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-gray-500">No transactions to display.</p>
              )}
            </div>

            <div className="flex flex-col sm:flex-row justify-center gap-4 mt-6">
              <button
                onClick={() => setCurrentPage('payment')}
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-blue-300"
              >
                Make a Payment
              </button>
              <button
                onClick={handleLogout}
                className="flex-1 bg-gray-400 hover:bg-gray-500 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-gray-300"
              >
                Logout
              </button>
            </div>
          </>
        )}

        {/* --- Payment Page UI --- */}
        {currentPage === 'payment' && currentUser && (
          <>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-indigo-800 mb-6">
              Make a Payment
            </h1>
            <p className="text-gray-600 mb-6">Current Balance: <span className="font-bold text-green-600">${currentUser.balance.toFixed(2)}</span></p>

            <div className="text-left mb-6">
              <label htmlFor="recipientId" className="block text-gray-700 text-sm font-bold mb-2">
                Recipient Account ID:
              </label>
              <input
                type="text"
                id="recipientId"
                className="w-full p-3 mb-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition-all duration-200"
                placeholder="e.g., ACC-002-B"
                value={paymentRecipientId}
                onChange={(e) => setPaymentRecipientId(e.target.value)}
                disabled={loading}
              />

              <label htmlFor="amount" className="block text-gray-700 text-sm font-bold mb-2">
                Amount:
              </label>
              <input
                type="number"
                id="amount"
                className="w-full p-3 mb-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition-all duration-200"
                placeholder="e.g., 100.00"
                value={paymentAmount}
                onChange={(e) => setPaymentAmount(e.target.value)}
                disabled={loading}
              />
            </div>

            <div className="flex flex-col sm:flex-row justify-center gap-4 mt-6">
              <button
                onClick={handleMakePayment}
                className="flex-1 bg-green-600 hover:bg-green-700 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-green-300 disabled:opacity-50"
                disabled={loading || !paymentRecipientId || !paymentAmount}
              >
                {loading ? 'Processing...' : 'Confirm Payment'}
              </button>
              <button
                onClick={() => setCurrentPage('dashboard')}
                className="flex-1 bg-gray-400 hover:bg-gray-500 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-gray-300"
                disabled={loading}
              >
                Back to Dashboard
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
