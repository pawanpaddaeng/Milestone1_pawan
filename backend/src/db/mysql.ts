import mysql from 'mysql2/promise';
import { env } from '../config/env';

// One pool for the whole app; mysql2 reuses connections from it.
export const pool = mysql.createPool(env.mysqlUrl);