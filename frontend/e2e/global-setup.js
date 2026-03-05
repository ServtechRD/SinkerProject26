import { execSync } from 'child_process'
import { readFileSync } from 'fs'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export default async function globalSetup() {
  if (process.env.SKIP_DB_SEED) {
    console.log('E2E: Skipping DB seed (SKIP_DB_SEED set)')
    return
  }

  const sqlPath = resolve(__dirname, 'fixtures', 'test-users.sql')
  const sql = readFileSync(sqlPath, 'utf-8')
    .split('\n')
    .filter(line => !line.trim().startsWith('--') && line.trim())
    .join(' ')

  const host = process.env.DB_HOST || 'localhost'

  try {
    execSync(
      `mariadb -h${host} -P3306 -uapp -papp app -e "${sql.replace(/"/g, '\\"')}"`,
      { stdio: 'pipe' }
    )
    console.log('E2E: Test users seeded successfully')
  } catch (err) {
    // Try via docker compose if direct connection fails
    try {
      const projectRoot = resolve(__dirname, '..', '..')
      execSync(
        `docker compose exec -T db mariadb -uapp -papp app -e "${sql.replace(/"/g, '\\"')}"`,
        { cwd: projectRoot, stdio: 'pipe' }
      )
      console.log('E2E: Test users seeded successfully (via docker compose)')
    } catch (err2) {
      console.error('E2E: Failed to seed test users:', err2.message)
      throw err2
    }
  }
}
