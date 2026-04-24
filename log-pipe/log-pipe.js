#!/usr/bin/env node

// log-pipe.js - Pipe terminal logs to the Log Aggregator
//
// Usage:
//   node app.js 2>&1 | node log-pipe.js --schema <name> [options]
//
// Options:
//   --schema <name>   Schema name (required) — must match a schema created in the dashboard
//   --url <url>       Gateway URL (default: http://localhost:8080)
//   --batch <n>       Batch size before flushing (default: 10)
//   --flush <ms>      Max time between flushes in ms (default: 2000)
//   --verbose         Log pipe activity to stderr
//   --help            Show this help message

const http = require('http');
const https = require('https');
const readline = require('readline');
const { URL } = require('url');

// ── Parse CLI Arguments ──

const args = process.argv.slice(2);
let schemaName = null;
let gatewayUrl = 'http://localhost:8080';
let batchSize = 10;
let flushIntervalMs = 2000;
let verbose = false;

for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
        case '--schema':
            schemaName = args[++i];
            break;
        case '--url':
            gatewayUrl = args[++i];
            break;
        case '--batch':
            batchSize = parseInt(args[++i], 10);
            break;
        case '--flush':
            flushIntervalMs = parseInt(args[++i], 10);
            break;
        case '--verbose':
            verbose = true;
            break;
        case '--help':
            console.error('Usage: <app> | node log-pipe.js --schema <name> [--url <gateway>] [--batch N] [--flush ms] [--verbose]');
            console.error('');
            console.error('Options:');
            console.error('  --schema <name>   Schema name (required)');
            console.error('  --url <url>       Gateway URL (default: http://localhost:8080)');
            console.error('  --batch <n>       Batch size before flush (default: 10)');
            console.error('  --flush <ms>      Flush interval in ms (default: 2000)');
            console.error('  --verbose         Show pipe activity on stderr');
            process.exit(0);
    }
}

if (!schemaName) {
    console.error('Error: --schema is required');
    console.error('Usage: <app> | node log-pipe.js --schema <name>');
    process.exit(1);
}

if (verbose) {
    console.error(`[log-pipe] schema=${schemaName} url=${gatewayUrl} batch=${batchSize} flush=${flushIntervalMs}ms`);
}

// ── Buffer & Flush ──

let buffer = [];
let flushTimer = null;
let totalSent = 0;
let totalIgnored = 0;

const rl = readline.createInterface({ input: process.stdin, terminal: false });

rl.on('line', (line) => {
    // Always pass through to stdout so the developer sees their logs
    process.stdout.write(line + '\n');

    // Try to parse as JSON, otherwise wrap as plain message
    let logObj;
    try {
        logObj = JSON.parse(line);
        if (typeof logObj !== 'object' || logObj === null || Array.isArray(logObj)) {
            logObj = { message: line };
        }
    } catch (e) {
        logObj = { message: line };
    }

    buffer.push(logObj);

    if (buffer.length >= batchSize) {
        flush();
    } else if (!flushTimer) {
        flushTimer = setTimeout(flush, flushIntervalMs);
    }
});

rl.on('close', () => {
    if (buffer.length > 0) flush();
    if (verbose) {
        console.error(`[log-pipe] Stream closed. Total sent: ${totalSent}, ignored: ${totalIgnored}`);
    }
});

function flush() {
    if (flushTimer) {
        clearTimeout(flushTimer);
        flushTimer = null;
    }
    if (buffer.length === 0) return;

    const batch = buffer.splice(0);
    const url = new URL(`/ingest/raw/${encodeURIComponent(schemaName)}/batch`, gatewayUrl);
    const client = url.protocol === 'https:' ? https : http;

    const body = JSON.stringify(batch);

    const req = client.request(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(body)
        }
    }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
            if (verbose) {
                try {
                    const parsed = JSON.parse(data);
                    totalSent += parsed.accepted || 0;
                    totalIgnored += parsed.ignored || 0;
                    console.error(`[log-pipe] Batch: ${batch.length} sent -> ${parsed.accepted} accepted, ${parsed.ignored} ignored`);
                } catch (e) {
                    console.error(`[log-pipe] Batch: ${batch.length} -> status ${res.statusCode}`);
                }
            }
        });
    });

    req.on('error', (err) => {
        if (verbose) {
            console.error(`[log-pipe] Error: ${err.message}`);
        }
        // Don't crash — silently continue
    });

    req.write(body);
    req.end();
}

// Handle SIGINT/SIGTERM gracefully
process.on('SIGINT', () => {
    if (buffer.length > 0) flush();
    process.exit(0);
});

process.on('SIGTERM', () => {
    if (buffer.length > 0) flush();
    process.exit(0);
});
