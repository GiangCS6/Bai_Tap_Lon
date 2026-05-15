# TODO - Auction seller put item on market

- [ ] Step 1: Update plan in code context (AuctionServer.java only)
- [ ] Step 2: Implement new client command handling: PUT_ITEM (and/or SELL_ITEM alias)
- [ ] Step 3: Add seller/role checking during LOGIN (Seller can list items)
- [ ] Step 4: Update SQLite schema for auction_items (add seller column if needed)
- [ ] Step 5: Implement DB persistence for new items (INSERT OR REPLACE)
- [ ] Step 6: Broadcast NEW_ITEM message to all connected clients
- [ ] Step 7: (Optional) Extend ClientHandler with sendPutItem method for protocol completeness
- [ ] Step 8: Run compile/tests (mvn test or mvn -q test/compile)

