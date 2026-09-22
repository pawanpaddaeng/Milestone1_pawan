import express, { type Express } from 'express';
import { OAuth2Client } from 'google-auth-library';
import os from 'os';

export function createApp(): Express {
  const app = express();
  app.use(express.json())
  const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

  interface GoogleAuthRequestBody {
    idToken: string;
  }

  app.post('/api/auth/google', async (req: Request<{}, {}, GoogleAuthRequestBody>, res: Response) => {
    const { idToken } = req.body;
    console.log("Received an authorization request");
    

    if (!idToken) {
      return res.status(400).json({ error: 'idToken is required' });
    }

    try {
      // Verify the token with Google
      const ticket = await googleClient.verifyIdToken({
        idToken: idToken,
        audience: process.env.GOOGLE_CLIENT_ID,
      });

      const payload = ticket.getPayload();

      if (!payload) {
        return res.status(401).json({ error: 'Invalid token payload' });
      }

      const firstName = payload.given_name || '';
      const lastName = payload.family_name || '';
      console.log(`Successfully authenticated user`);

      return res.status(200).json({
        message: 'Authentication successful',
        user: {
          firstName,
          lastName,
        },
      });
    } catch (error) {
      console.error('Token verification failed:', error);
      return res.status(401).json({ error: 'Invalid Google ID token' });
    }
  });

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/getIP4', async (_req, res) => {
    //const networkInterfaces = os.networkInterfaces();
    let address = '127.0.0.1'
    try{
      console.log("Trying fetch")
      const publicIp = await fetch('https://icanhazip.com')
          .then((res) => res.text())
          .then((text) => text.trim());
      console.log("GOT" + publicIp)
      address = publicIp
      return res.status(200).json({
        msg: address,
      });
    } catch (error) {
      return res.status(401).json({
        msg: error,
      });
    }
    // for (const interfaceName of Object.keys(networkInterfaces)) {
    //   const interfaces = networkInterfaces[interfaceName];
    //   if (!interfaces) continue;

    //   for (const iface of interfaces) {
    //     // Skip internal (127.0.0.1) and non-IPv4 addresses
    //     if (iface.family === 'IPv4' && !iface.internal) {
    //       address = iface.address;
    //     }
    //   }
    // }
    // return res.status(200).json({
    //     msg: address,
    //   });
  });

  app.get('/getTime', async (_req, res) => {
    const now = new Date();

    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const timeStr = `${hours}:${minutes}:${seconds}`;

    //get time offset from utc then convert to hh:mm
    const offsetMinutesTotal = now.getTimezoneOffset();
    const sign = offsetMinutesTotal <= 0 ? '+' : '-';
    const absOffset = Math.abs(offsetMinutesTotal);

    const offsetHours = String(Math.floor(absOffset / 60)).padStart(2, '0');
    const offsetMins = String(absOffset % 60).padStart(2, '0');
    const gmtOffsetStr = `GMT${sign}${offsetHours}:${offsetMins}`;

    res.status(200).json({msg:`${timeStr} ${gmtOffsetStr}`})
  })

  app.get('/getAuthorName', async(_req, res) => {
    res.status(200).json({
          firstName: "Pawanpreet",
          lastName: "Padda"
      });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
