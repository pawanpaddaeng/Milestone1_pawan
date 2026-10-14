import type { RowDataPacket, ResultSetHeader } from 'mysql2';
import { pool } from './mysql';

export interface User {
  user_id: number;
  first_name: string;
  last_name: string;
}

export async function createUser(firstName: string, lastName: string): Promise<number> {
  const [result] = await pool.execute<ResultSetHeader>(
    'INSERT INTO users (first_name, last_name) VALUES (?, ?)',
    [firstName, lastName],
  );
  return result.insertId;
}