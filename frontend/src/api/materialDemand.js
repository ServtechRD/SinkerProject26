import api from './axios'

export function getMaterialDemand(weekStart, factory) {
  return api.get(`/api/material-demand?week_start=${weekStart}&factory=${factory}`).then((r) => r.data)
}
