#!/bin/bash

# Firebase Cloud Functions Deployment Script
# Run this script in your terminal to deploy the notification functions

set -e

echo "🔥 Firebase Cloud Functions Deployment"
echo "========================================"
echo ""

cd "$(dirname "$0")"

# Check if logged in
if ! npx firebase login:list 2>/dev/null | grep -q "@"; then
    echo "📱 Step 1: Login to Firebase"
    echo "   A browser window will open for authentication..."
    echo ""
    npx firebase login
fi

echo ""
echo "✅ Logged in to Firebase"
echo ""

# Initialize if firebase.json doesn't exist
if [ ! -f "../firebase.json" ]; then
    echo "📋 Step 2: Initializing Firebase project..."
    cd ..
    npx firebase-functions/node_modules/.bin/firebase init functions --project YOUR-PROJECT-ID
    cd firebase-functions
else
    echo "📋 Firebase already initialized"
fi

echo ""
echo "🚀 Step 3: Deploying Cloud Functions..."
echo ""

# Deploy
npx firebase deploy --only functions --project YOUR-PROJECT-ID

echo ""
echo "✅ Deployment Complete!"
echo ""
echo "Your push notifications will now work automatically when messages are created in Firestore."
echo ""
echo "View logs: npx firebase functions:log --project YOUR-PROJECT-ID"

