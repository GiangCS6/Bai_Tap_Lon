/*
package bai_tap_lon.server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')),
                    isActive BOOLEAN DEFAULT 1,
                    is_deleted BOOLEAN DEFAULT 0,
                    balance INTEGER DEFAULT 0,
                    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    starting_price INTEGER DEFAULT 0,
                    category TEXT,
                    seller_id TEXT,
                    image_url TEXT,
                    attributes TEXT,
                    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Auctions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id TEXT PRIMARY KEY,
                    item_id TEXT,
                    status TEXT DEFAULT 'ACTIVE',
                    current_price INTEGER DEFAULT 0,
                    start_time TEXT,
                    end_time TEXT,
                    winner_id TEXT,
                    is_deleted BOOLEAN DEFAULT 0,
                    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
                )
            """);

            // Bids table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bids (
                    id TEXT PRIMARY KEY,
                    auction_id TEXT,
                    bidder_id TEXT,
                    amount INTEGER NOT NULL,
                    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Watchlist
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS watchlist (
                    user_id TEXT,
                    auction_id TEXT,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, auction_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
                )
            """);

            System.out.println("Database tables initialized!");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}*/
package bai_tap_lon.server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println(" Initializing database tables...");

            // Users
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    role VARCHAR(255) NOT NULL CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')),
                    isActive BOOLEAN DEFAULT 1,
                    is_deleted BOOLEAN DEFAULT 0,
                    balance INTEGER DEFAULT 0,
                    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Items - IMPORTANT: must have item_name
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id VARCHAR(255) PRIMARY KEY,
                    item_name VARCHAR(255) NOT NULL,
                    description VARCHAR(255),
                    starting_price INTEGER DEFAULT 0,
                    category VARCHAR(255),
                    seller_id VARCHAR(255),
                    image_Url VARCHAR(255),
                    attributes VARCHAR(255),
                    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Auctions
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id VARCHAR(255) PRIMARY KEY,
                    item_id VARCHAR(255),
                    status VARCHAR(255) DEFAULT 'OPEN',
                    current_price INTEGER DEFAULT 0,
                    start_time VARCHAR(255),
                    end_time VARCHAR(255),
                    winner_id VARCHAR(255),
                    is_deleted BOOLEAN DEFAULT 0,
                    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
                )
            """);

            // Bids
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bids (
                    id VARCHAR(255) PRIMARY KEY,
                    auction_id VARCHAR(255),
                    bidder_id VARCHAR(255),
                    amount INTEGER NOT NULL,
                    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Watchlist
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_watched_auctions (
                    user_id VARCHAR(255),
                    auction_id VARCHAR(255),
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, auction_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
                )
            """);

            // Auto Bid Settings - Added this block
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auto_bid_settings (
                    id VARCHAR(255) PRIMARY KEY,
                    bidder_id VARCHAR(255) NOT NULL,
                    auction_id VARCHAR(255) NOT NULL,
                    max_bid INTEGER NOT NULL,
                    increment INTEGER NOT NULL DEFAULT 1000,
                    is_active BOOLEAN DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(bidder_id, auction_id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
                )
            """);


            System.out.println("Database tables initialized successfully!");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}