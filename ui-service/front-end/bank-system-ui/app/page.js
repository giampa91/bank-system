'use client'; // This directive makes this component a Client Component

import React, { useState } from 'react';

// Mock data for users and their accounts
const mockUsers = [
  {
    id: 'user123',
    name: 'Alice Johnson',
    balance: 5432.10,
    transactions: [
      { id: 't1', date: '2025-06-01', description: 'Grocery Store', amount: -75.50, type: 'debit' },
      { id: 't2', date: '2025-05-28', description: 'Salary Deposit', amount: 2500.00, type: 'credit' },
      { id: 't3', date: '2025-05-25', description: 'Online Subscription', amount: -12.99, type: 'debit' },
      { id: 't4', date: '2025-05-20', description: 'Electricity Bill', amount: -120.00, type: 'debit' },
    ],
  },
  {
    id: 'user456',
    name: 'Bob Smith',
    balance: 1234.56,
    transactions: [
      { id: 't5', date: '2025-06-02', description: 'Restaurant', amount: -45.00, type: 'debit' },
      { id: 't6', date: '2025-05-30', description: 'Freelance Payment', amount: 800.00, type: 'credit' },
      { id: 't7', date: '2025-05-26', description: 'Cinema Tickets', amount: -30.00, type: 'debit' },
    ],
  },
];

// --- Mock API Functions (simulating Axios calls) ---
// In a real Next.js app, these would typically be actual API calls to a backend
// or Next.js API Routes (e.g., using axios.get('/api/user', { params: { userId } }))

// Simulate fetching user info
const mockFetchUserInfo = (userId) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const user = mockUsers.find(u => u.id === userId);
      if (user) {
        resolve({ data: { id: user.id, name: user.name } }); // Only return basic info
      } else {
        reject(new Error('User not found'));
      }
    }, 1000); // Simulate network delay
  });
};

// Simulate fetching dashboard data (balance and transactions)
const mockFetchDashboardData = (userId) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const user = mockUsers.find(u => u.id === userId);
      if (user) {
        resolve({ data: { balance: user.balance, transactions: user.transactions } });
      } else {
        reject(new Error('Dashboard data not found'));
      }
    }, 1200); // Simulate network delay
  });
};

// Simulate making a payment
const mockMakePayment = (payerId, recipientId, amount) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const payer = mockUsers.find(u => u.id === payerId);
      const recipient = mockUsers.find(u => u.id === recipientId);

      if (!payer) {
        reject(new Error('Payer not found.'));
        return;
      }
      if (!recipient) {
        reject(new Error('Recipient not found.'));
        return;
      }
      if (payer.balance < amount) {
        reject(new Error('Insufficient funds.'));
        return;
      }
      if (amount <= 0) {
        reject(new Error('Amount must be positive.'));
        return;
      }

      // Simulate balance update (in a real app, this would be backend logic)
      payer.balance -= amount;
      recipient.balance += amount;

      // Add dummy transaction for payer
      payer.transactions.unshift({
        id: `t${Date.now()}`,
        date: new Date().toISOString().split('T')[0],
        description: `Payment to ${recipient.name}`,
        amount: -amount,
        type: 'debit',
      });

      resolve({ data: { message: 'Payment successful!' } });
    }, 1500); // Simulate network delay
  });
};

// Home component for app/page.js
export default function Home() {
  const [currentPage, setCurrentPage] = useState('login'); // 'login', 'dashboard', 'payment'
  const [currentUser, setCurrentUser] = useState(null); // Stores { id, name, balance, transactions }
  const [userIdInput, setUserIdInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(''); // General message for success/error

  // --- Login Page Logic ---
  const handleLogin = async () => {
    setLoading(true);
    setMessage('');
    try {
      // Simulate API call to get user info
      const response = await mockFetchUserInfo(userIdInput);
      const userInfo = response.data; // { id, name }

      // Simulate fetching dashboard data after successful login
      const dashboardResponse = await mockFetchDashboardData(userInfo.id);
      setCurrentUser({
        ...userInfo,
        balance: dashboardResponse.data.balance,
        transactions: dashboardResponse.data.transactions,
      });
      setCurrentPage('dashboard'); // Navigate to dashboard
    } catch (error) {
      setMessage(`Login failed: ${error.message}. Try 'user123' or 'user456'.`);
      setCurrentUser(null);
    } finally {
      setLoading(false);
    }
  };

  // --- Payment Page Logic ---
  const [paymentRecipientId, setPaymentRecipientId] = useState('');
  const [paymentAmount, setPaymentAmount] = useState('');

  const handleMakePayment = async () => {
    setLoading(true);
    setMessage('');
    try {
      const amount = parseFloat(paymentAmount);
      if (isNaN(amount) || amount <= 0) {
        setMessage('Please enter a valid positive amount.');
        setLoading(false);
        return;
      }

      // Simulate API call to make payment
      const response = await mockMakePayment(currentUser.id, paymentRecipientId, amount);
      setMessage(response.data.message);

      // Refresh dashboard data after successful payment
      const dashboardResponse = await mockFetchDashboardData(currentUser.id);
      setCurrentUser(prevUser => ({
        ...prevUser,
        balance: dashboardResponse.data.balance,
        transactions: dashboardResponse.data.transactions,
      }));
      setPaymentRecipientId('');
      setPaymentAmount('');
    } catch (error) {
      setMessage(`Payment failed: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setUserIdInput('');
    setMessage('');
    setCurrentPage('login');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center p-4 font-inter">
      <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-lg sm:max-w-xl text-center">
        {/* Loading Indicator */}
        {loading && (
          <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center rounded-xl z-10">
            <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-indigo-500"></div>
            <p className="ml-4 text-indigo-700 text-lg">Loading...</p>
          </div>
        )}

        {/* Message Display */}
        {message && (
          <div className={`mb-4 p-3 rounded-lg text-white ${message.includes('failed') || message.includes('error') ? 'bg-red-500' : 'bg-green-500'}`}>
            {message}
          </div>
        )}

        {/* --- Login Page --- */}
        {currentPage === 'login' && (
          <>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-indigo-800 mb-6">
              Welcome to Your Bank
            </h1>
            <p className="text-gray-600 mb-6">Enter a user ID to login. Try `user123` or `user456`.</p>
            <input
              type="text"
              className="w-full p-3 mb-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition-all duration-200"
              placeholder="Enter User ID (e.g., user123)"
              value={userIdInput}
              onChange={(e) => setUserIdInput(e.target.value)}
              disabled={loading}
            />
            <button
              onClick={handleLogin}
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 px-6 rounded-lg shadow-md transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-indigo-300 disabled:opacity-50"
              disabled={loading || !userIdInput}
            >
              {loading ? 'Logging In...' : 'Login'}
            </button>
          </>
        )}

        {/* --- Dashboard Page --- */}
        {currentPage === 'dashboard' && currentUser && (
          <>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-indigo-800 mb-6">
              Welcome, {currentUser.name}!
            </h1>
            <div className="mb-8 text-left">
              <p className="text-xl font-semibold text-gray-700 mb-2">Account Balance:</p>
              <p className="text-5xl font-bold text-green-600 mb-6">
                ${currentUser.balance.toFixed(2)}
              </p>

              <h2 className="text-2xl font-semibold text-indigo-700 mb-4">Recent Transactions</h2>
              {currentUser.transactions.length > 0 ? (
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

        {/* --- Payment Page --- */}
        {currentPage === 'payment' && currentUser && (
          <>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-indigo-800 mb-6">
              Make a Payment
            </h1>
            <p className="text-gray-600 mb-6">Current Balance: <span className="font-bold text-green-600">${currentUser.balance.toFixed(2)}</span></p>

            <div className="text-left mb-6">
              <label htmlFor="recipientId" className="block text-gray-700 text-sm font-bold mb-2">
                Recipient User ID:
              </label>
              <input
                type="text"
                id="recipientId"
                className="w-full p-3 mb-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition-all duration-200"
                placeholder="e.g., user456"
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
