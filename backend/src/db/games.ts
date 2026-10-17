import type { ResultSetHeader } from 'mysql2';
import { pool } from './mysql';

export type SkillLevel = 'any' | 'beginner' | 'intermediate' | 'advanced';

// fields needed to create a game, matching the games table
export interface NewGame {
  organizerId: number;
  title: string;
  sport: string;
  description: string | null;
  startsAt: Date;
  endsAt: Date | null;
  locationName: string;
  address: string;
  latitude: number;
  longitude: number;
  maxPlayers: number;
  skillLevel: SkillLevel;
}

// games stores times as UTC DATETIME values ("YYYY-MM-DD HH:MM:SS")
function toUtcDatetime(date: Date): string {
  return date.toISOString().slice(0, 19).replace('T', ' ');
}

/*  creates the game and adds the organizer as its first player in one
    transaction, so a game never exists without its organizer. Returns game_id
 */
export async function createGame(game: NewGame): Promise<number> {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const [result] = await connection.execute<ResultSetHeader>(
      `INSERT INTO games (organizer_id, title, sport, description, starts_at, ends_at,
                          location_name, address, latitude, longitude, max_players, skill_level)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        game.organizerId, game.title, game.sport, game.description,
        toUtcDatetime(game.startsAt), game.endsAt ? toUtcDatetime(game.endsAt) : null,
        game.locationName, game.address, game.latitude, game.longitude,
        game.maxPlayers, game.skillLevel,
      ],
    );
    await connection.execute<ResultSetHeader>(
      'INSERT INTO game_players (game_id, user_id) VALUES (?, ?)',
      [result.insertId, game.organizerId],
    );

    await connection.commit();
    return result.insertId;
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}
