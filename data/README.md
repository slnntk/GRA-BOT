# Pasta para os Datasets Olist

Esta pasta deve conter os seguintes arquivos CSV da Olist:

## Arquivos Obrigatórios:

1. **olist_orders_dataset.csv**
   - Informações principais dos pedidos
   - Campos: order_id, customer_id, order_status, datas, etc.

2. **olist_order_items_dataset.csv** 
   - Itens dos pedidos
   - Campos: order_id, product_id, seller_id, price, etc.

3. **olist_order_payments_dataset.csv**
   - Informações de pagamento
   - Campos: order_id, payment_type, payment_value, etc.

4. **olist_order_reviews_dataset.csv**
   - Avaliações dos clientes
   - Campos: order_id, review_score, review_comment, etc.

5. **olist_products_dataset.csv**
   - Informações dos produtos  
   - Campos: product_id, product_category_name, dimensões, etc.

6. **product_category_name_translation.csv**
   - Tradução dos nomes das categorias
   - Campos: product_category_name, product_category_name_english

## Como Obter os Dados:

1. Acesse o [Kaggle Dataset Olist](https://www.kaggle.com/olistbr/brazilian-ecommerce)
2. Baixe o arquivo ZIP completo
3. Extraia os 6 arquivos CSV listados acima
4. Coloque-os nesta pasta `data/`

## Validação:

Execute o script de validação para verificar se todos os arquivos estão presentes:
```bash
python validate_data.py
```

---
**Nota**: Os arquivos de dados não são versionados no Git devido ao tamanho. Baixe-os diretamente da fonte oficial.