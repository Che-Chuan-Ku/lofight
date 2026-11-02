// Shop tag (specs/api.yml).
import { apiRequest } from './client.js';

export const listShopItems = () => apiRequest('listShopItems');
export const purchaseShopItem = ({ shopItemId }) => apiRequest('purchaseShopItem', { pathParams: { shopItemId } });
