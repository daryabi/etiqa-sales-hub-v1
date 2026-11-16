import type { Page, PageQuery } from '../types';
import { getJson, pageQueryString, postJson } from './httpClient';

export function createCrudApi<TEntity, TNew>(resourcePath: string) {
  return {
    list: (query: PageQuery) => getJson<Page<TEntity>>(`${resourcePath}?${pageQueryString(query)}`),
    create: (payload: TNew) => postJson<TEntity>(resourcePath, payload),
  };
}
