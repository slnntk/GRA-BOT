#!/usr/bin/env python3
"""
Script para validar se todos os arquivos necessários para a análise Olist estão presentes.
"""

import os
import sys
from pathlib import Path

def validate_olist_data():
    """Valida se todos os arquivos CSV necessários estão presentes na pasta data/"""
    
    required_files = [
        'olist_orders_dataset.csv',
        'olist_order_items_dataset.csv', 
        'olist_order_payments_dataset.csv',
        'olist_order_reviews_dataset.csv',
        'olist_products_dataset.csv',
        'product_category_name_translation.csv'
    ]
    
    data_path = Path('./data')
    
    print("🔍 Validando arquivos da Olist...")
    print("=" * 50)
    
    missing_files = []
    present_files = []
    
    for file_name in required_files:
        file_path = data_path / file_name
        if file_path.exists():
            file_size = file_path.stat().st_size / (1024 * 1024)  # MB
            present_files.append((file_name, file_size))
            print(f"✅ {file_name:<40} ({file_size:.1f} MB)")
        else:
            missing_files.append(file_name)
            print(f"❌ {file_name:<40} (Não encontrado)")
    
    print("=" * 50)
    
    if not missing_files:
        print("🎉 SUCESSO! Todos os arquivos estão presentes.")
        print(f"📁 Total de arquivos: {len(present_files)}")
        total_size = sum(size for _, size in present_files)
        print(f"💾 Tamanho total: {total_size:.1f} MB")
        print("\n✨ Você pode executar o notebook 'olist_analysis_principal.ipynb' agora!")
        return True
    else:
        print(f"⚠️  ATENÇÃO! {len(missing_files)} arquivo(s) faltando:")
        for file_name in missing_files:
            print(f"   - {file_name}")
        
        print("\n📋 PRÓXIMOS PASSOS:")
        print("1. Acesse: https://www.kaggle.com/olistbr/brazilian-ecommerce")
        print("2. Baixe o dataset completo")
        print("3. Extraia os arquivos CSV para a pasta 'data/'")
        print("4. Execute este script novamente para validar")
        
        return False

def create_sample_data():
    """Cria arquivos CSV vazios como exemplo da estrutura esperada"""
    print("\n🔨 Criando arquivos de exemplo...")
    
    sample_structures = {
        'olist_orders_dataset.csv': 'order_id,customer_id,order_status,order_purchase_timestamp,order_approved_at,order_delivered_carrier_date,order_delivered_customer_date,order_estimated_delivery_date',
        'olist_order_items_dataset.csv': 'order_id,order_item_id,product_id,seller_id,shipping_limit_date,price,freight_value',
        'olist_order_payments_dataset.csv': 'order_id,payment_sequential,payment_type,payment_installments,payment_value',
        'olist_order_reviews_dataset.csv': 'review_id,order_id,review_score,review_comment_title,review_comment_message,review_creation_date,review_answer_timestamp',
        'olist_products_dataset.csv': 'product_id,product_category_name,product_name_lenght,product_description_lenght,product_photos_qty,product_weight_g,product_length_cm,product_height_cm,product_width_cm',
        'product_category_name_translation.csv': 'product_category_name,product_category_name_english'
    }
    
    data_path = Path('./data')
    data_path.mkdir(exist_ok=True)
    
    for file_name, headers in sample_structures.items():
        sample_file = data_path / f"sample_{file_name}"
        with open(sample_file, 'w') as f:
            f.write(headers + '\n')
        print(f"📄 Criado: sample_{file_name}")
    
    print("✨ Arquivos de exemplo criados! Use-os como referência para a estrutura esperada.")

if __name__ == "__main__":
    print("🔍 VALIDADOR DE DADOS OLIST")
    print("=" * 50)
    
    if not validate_olist_data():
        print("\n❓ Deseja criar arquivos de exemplo? (s/n): ", end="")
        response = input().lower().strip()
        if response in ['s', 'sim', 'y', 'yes']:
            create_sample_data()
        
        sys.exit(1)
    
    sys.exit(0)