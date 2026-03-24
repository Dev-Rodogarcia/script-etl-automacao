---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: legado
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
{
	"info": {
		"_postman_id": "1efd2408-934b-42ee-b09a-8f3afc97cafb",
		"name": "Rodogarcia",
		"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
		"_exporter_id": "39996087"
	},
	"item": [
		{
			"name": "DATA EXPORT",
			"item": [
				{
					"name": "1) Consulta todos os templates",
					"request": {
						"auth": {
							"type": "bearer",
							"bearer": [
								{
									"key": "token",
									"value": "***TOKEN_CENSURADO_POR_SEGURANCA***",
									"type": "string"
								}
							]
						},
						"method": "GET",
						"header": [],
						"url": {
							"raw": "https://rodogarcia.eslcloud.com.br/api/analytics/reports",
							"protocol": "https",
							"host": [
								"rodogarcia",
								"eslcloud",
								"com",
								"br"
							],
							"path": [
								"api",
								"analytics",
								"reports"
							],
							"query": [
								{
									"key": "per",
									"value": "100",
									"description": "Quantidade de registros por paginação, podendo variar de 1 (um) até 100 (cem). Quando não informado o parâmetro per, por padrão o valor dele é 12 (doze).",
									"disabled": true
								},
								{
									"key": "page",
									"value": "1",
									"description": "Parâmetro de paginação, ao realizar o GET da consulta pode ser passado o parâmetro incrementando de um em um até que não seja retornado nenhum registro.",
									"disabled": true
								}
							]
						},
						"description": "A requisição para realizar a **consulta dos templates do Data Export**, deve ser feita **utilizando o método GET** e especificando no endereço o _tenant_ correspondente, a mesma pode ser realizada a partir de um ou mais parâmetros opcionais, que podem ser combinados de acordo com a necessidade da consulta, abaixo é possível visualizar a definição dos parâmetros aceitos nessa API.\n\n[Observações:](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#0ae0ce29-746a-4fd7-ba70-9f2d5ea432f0)\n\n- Todas as requisições devem respeitar um intervalo de 2 segundos entre elas, se utilizado o mesmo endereço IP. Caso contrário, o status code 429 Too Many Requests será retornado;\n    \n    - Para diferentes IP’s não é necessário o intervalo de 2 segundos;\n        \n    - Se no período consultado a data for inferior a 31 dias, o sistema permite solicitar quantas vezes for necessário sem impor limites de horas, porém, seguindo a regra citada anteriormente;\n        \n    - Se no período consultado a data estiver entre 31 dias e 6 meses, o sistema limitará por 1 hora a nova extração de dados;\n        \n    - Se no período consultado a data estiver acima de 6 meses, o sistema limitará por 12 horas a nova extração de dados.\n        \n- **Token**: Para utilização da API é necessário gerar o Token, e com a finalidade de facilitar a utilização das nossas ferramentas temos uma Wiki, nela é possível encontrar detalhes de como gerar o [Wiki token API.](https://eslcloud.zendesk.com/hc/pt-br/articles/20492545919885--Gera%C3%A7%C3%A3o-Token-Token-API-Cadastro-Token-API)\n    \n- **endPoint:** Alterar a palavra subdominio para o ([<i>Tenant</i>](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#6cc933c1-294b-46d8-99a8-d581ea8f6bc6)) da transportadora.\n    \n\n_Atualizado 08/07/2024_"
					},
					"response": [
						{
							"name": "1) Consulta todos os templates",
							"originalRequest": {
								"method": "GET",
								"header": [],
								"url": {
									"raw": "{{url_base}}/api/analytics/reports",
									"host": [
										"{{url_base}}"
									],
									"path": [
										"api",
										"analytics",
										"reports"
									],
									"query": [
										{
											"key": "per",
											"value": "100",
											"description": "Quantidade de registros por paginação, podendo variar de 1 (um) até 100 (cem). Quando não informado o parâmetro per, por padrão o valor dele é 12 (doze).",
											"disabled": true
										},
										{
											"key": "page",
											"value": "1",
											"description": "Parâmetro de paginação, ao realizar o GET da consulta pode ser passado o parâmetro incrementando de um em um até que não seja retornado nenhum registro.",
											"disabled": true
										}
									]
								}
							},
							"status": "OK",
							"code": 200,
							"_postman_previewlanguage": "json",
							"header": [
								{
									"key": "Server",
									"value": "Cowboy"
								},
								{
									"key": "Date",
									"value": "Tue, 03 Jan 2023 11:42:01 GMT"
								},
								{
									"key": "Connection",
									"value": "keep-alive"
								},
								{
									"key": "X-Frame-Options",
									"value": "SAMEORIGIN"
								},
								{
									"key": "X-Xss-Protection",
									"value": "1; mode=block"
								},
								{
									"key": "X-Content-Type-Options",
									"value": "nosniff"
								},
								{
									"key": "X-Download-Options",
									"value": "noopen"
								},
								{
									"key": "X-Permitted-Cross-Domain-Policies",
									"value": "none"
								},
								{
									"key": "Referrer-Policy",
									"value": "strict-origin-when-cross-origin"
								},
								{
									"key": "Content-Type",
									"value": "application/json; charset=utf-8"
								},
								{
									"key": "Vary",
									"value": "Accept-Encoding, Origin"
								},
								{
									"key": "Content-Encoding",
									"value": "gzip"
								},
								{
									"key": "Etag",
									"value": "W/\"8b3b2d59090624ea2f8b27d31ddafd53\""
								},
								{
									"key": "Cache-Control",
									"value": "max-age=0, private, must-revalidate"
								},
								{
									"key": "X-Meta-Request-Version",
									"value": "0.7.2"
								},
								{
									"key": "X-Request-Id",
									"value": "187dd24c-f96e-434c-a7b2-9a58db14970a"
								},
								{
									"key": "X-Runtime",
									"value": "0.083908"
								},
								{
									"key": "Transfer-Encoding",
									"value": "chunked"
								},
								{
									"key": "Via",
									"value": "1.1 vegur"
								}
							],
							"cookie": [],
							"body": "[\n    //Esse retorno da API de consulta dos templates, apresenta todos os templates criados no sistema.\n    //Os nomes dos templates que retornam, podem variar de acordo com a estrutura do template criado pelo usuário, já que o mesmo tem a opção de edita-lo a sua própria conveniência.\n    \n    //Abaixo é apresentado um exemplo de consulta para que seja possível compreender a estrutura do resultado esperado.\n    {\n        //ID do template\n        \"id\": 1,\n        //Nome do template\n        \"name\": \"Fretes\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"freight\"\n    },\n    {\n        //ID do template\n        \"id\": 2,\n        //Nome do template\n        \"name\": \"Teste Frete\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"freight\"\n    },\n    {\n        //ID do template\n        \"id\": 3,\n        //Nome do template\n        \"name\": \"Teste Manifesto\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"manifest\"\n    },\n    {\n        //ID do template\n        \"id\": 4,\n        //Nome do template\n        \"name\": \"Teste Cotação\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"quote\"\n    },\n    {\n        //ID do template\n        \"id\": 5,\n        //Nome do template\n        \"name\": \"Teste Coleta\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"pick\"\n    },\n    {\n        //ID do template\n        \"id\": 6,\n        //Nome do template\n        \"name\": \"Teste Minuta Aéreo\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"dispatch_draft_aerial\"\n    },\n    {\n        //ID do template\n        \"id\": 7,\n        //Nome do template\n        \"name\": \"Teste Minuta Rodoviário\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"dispatch_draft_road\"\n    },\n    {\n        //ID do template\n        \"id\": 8,\n        //Nome do template\n        \"name\": \"Teste Consolidação Aérea\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"consolidation_aerial\"\n    },\n    {\n        //ID do template\n        \"id\": 9,\n        //Nome do template\n        \"name\": \"Teste Consolidação Rodoviária\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"consolidation_road\"\n    },\n    {\n        //ID do template\n        \"id\": 10,\n        //Nome do template\n        \"name\": \"Teste Veículo\",\n        //Raiz do template na qual ele busca a base dos dados.\n        \"root\": \"vehicle\"\n    },\n    {\n        //ID do template\n        \"id\": 11,\n        //Nome do template\n        \"name\": \"Teste Fatura a Receber\",\n        //Raiz do template na qual ele busca a base dos dados.            \n        \"root\": \"accounting_credit\"\n    },\n    {\n        //ID do template\n        \"id\": 455,\n        //Nome do template\n        \"name\": \"Ocorrencias\",\n        //Raiz do template na qual ele busca a base dos dados.    \n        \"root\": \"invoice_occurrence\"\n    }\n]"
						}
					]
				},
				{
					"name": "2) Consulta estrutura dos templates",
					"request": {
						"auth": {
							"type": "bearer",
							"bearer": [
								{
									"key": "token",
									"value": "***TOKEN_CENSURADO_POR_SEGURANCA***",
									"type": "string"
								}
							]
						},
						"method": "GET",
						"header": [],
						"url": {
							"raw": "https://rodogarcia.eslcloud.com.br/api/analytics/reports/6399/info",
							"protocol": "https",
							"host": [
								"rodogarcia",
								"eslcloud",
								"com",
								"br"
							],
							"path": [
								"api",
								"analytics",
								"reports",
								"6399",
								"info"
							]
						},
						"description": "A requisição para realizar a **consulta da estrutura dos templates do Data Export**, deve ser feita **utilizando o método GET** e especificando no endereço o _tenant_ correspondente, para realizar a consulta é **necessário passar como parâmetro o ID do template** que deseja buscar, **na URL**, esse **ID é encontrado no retorno da consulta dos templates**.\n\n[Observações:](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#0ae0ce29-746a-4fd7-ba70-9f2d5ea432f0)\n\n- Todas as requisições devem respeitar um intervalo de 2 segundos entre elas, se utilizado o mesmo endereço IP. Caso contrário, o status code 429 Too Many Requests será retornado;\n    \n    - Para diferentes IP’s não é necessário o intervalo de 2 segundos;\n        \n    - Se no período consultado a data for inferior a 31 dias, o sistema permite solicitar quantas vezes for necessário sem impor limites de horas, porém, seguindo a regra citada anteriormente;\n        \n    - Se no período consultado a data estiver entre 31 dias e 6 meses, o sistema limitará por 1 hora a nova extração de dados;\n        \n    - Se no período consultado a data estiver acima de 6 meses, o sistema limitará por 12 horas a nova extração de dados.\n        \n- **Token**: Para utilização da API é necessário gerar o Token, e com a finalidade de facilitar a utilização das nossas ferramentas temos uma Wiki, nela é possível encontrar detalhes de como gerar o [Wiki token API.](https://eslcloud.zendesk.com/hc/pt-br/articles/20492545919885--Gera%C3%A7%C3%A3o-Token-Token-API-Cadastro-Token-API)\n    \n- **endPoint:** Alterar a palavra subdominio para o ([<i>Tenant</i>](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#6cc933c1-294b-46d8-99a8-d581ea8f6bc6)) da transportadora.\n    \n\n_Atualizado 08/07/2024_"
					},
					"response": [
						{
							"name": "2) Retorno -  Consulta estrutura dos templates",
							"originalRequest": {
								"method": "GET",
								"header": [],
								"url": {
									"raw": "http://beta1.tmsweb.com.br/api/analytics/reports/3/info",
									"protocol": "http",
									"host": [
										"beta1",
										"tmsweb",
										"com",
										"br"
									],
									"path": [
										"api",
										"analytics",
										"reports",
										"3",
										"info"
									]
								}
							},
							"status": "OK",
							"code": 200,
							"_postman_previewlanguage": "json",
							"header": [
								{
									"key": "Server",
									"value": "Cowboy"
								},
								{
									"key": "Date",
									"value": "Mon, 10 Oct 2022 17:53:37 GMT"
								},
								{
									"key": "Connection",
									"value": "keep-alive"
								},
								{
									"key": "X-Frame-Options",
									"value": "SAMEORIGIN"
								},
								{
									"key": "X-Xss-Protection",
									"value": "1; mode=block"
								},
								{
									"key": "X-Content-Type-Options",
									"value": "nosniff"
								},
								{
									"key": "X-Download-Options",
									"value": "noopen"
								},
								{
									"key": "X-Permitted-Cross-Domain-Policies",
									"value": "none"
								},
								{
									"key": "Referrer-Policy",
									"value": "strict-origin-when-cross-origin"
								},
								{
									"key": "Content-Type",
									"value": "application/json; charset=utf-8"
								},
								{
									"key": "Vary",
									"value": "Accept-Encoding, Origin"
								},
								{
									"key": "Content-Encoding",
									"value": "gzip"
								},
								{
									"key": "Etag",
									"value": "W/\"cbf93891a683cdf8777fc3eefcd8895d\""
								},
								{
									"key": "Cache-Control",
									"value": "max-age=0, private, must-revalidate"
								},
								{
									"key": "X-Meta-Request-Version",
									"value": "0.7.2"
								},
								{
									"key": "X-Request-Id",
									"value": "62ab2357-ef72-4148-89a7-27fdbe37c373"
								},
								{
									"key": "X-Runtime",
									"value": "0.107071"
								},
								{
									"key": "Transfer-Encoding",
									"value": "chunked"
								},
								{
									"key": "Via",
									"value": "1.1 vegur"
								}
							],
							"cookie": [],
							"body": "{\n    //O retorno dessa consulta apresenta os CAMPOS da estrutura do template (Colunas da tabela), \n    //Os nomes e valores dos campos que retornam, podem variar de acordo com a estrutura do template criado pelo usuário, já que o mesmo tem a opção de edita-lo a sua própria conveniência.\n    \n    //Abaixo é apresentado um exemplo de consulta para que seja possível compreender a estrutura do resultado esperado.\n    \"fields\": {\n        //Dados da coluna ID\n        \"id\": {\n            //Nome do campo\n            \"title\": \"ID\",\n            //Tipo de valor que o campo retorna\n            \"filter\": \"integer\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna ID da filial de origem\n        \"mft_crn_id\": {\n            //Nome do campo\n            \"title\": \"Filial origem/ID\",\n            //Tipo de valor que o campo retorna\n            \"filter\": \"integer\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna filial de origem\n        \"mft_crn_psn_name\": {\n            //Nome do campo\n            \"title\": \"Filial Origem\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Motorista\n        \"mft_mdr_iil_name\": {\n            //Nome do campo\n            \"title\": \"Motorista\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Veículo/Placa\n        \"mft_vie_license_plate\": {\n            //Nome do campo\n            \"title\": \"Veículo/Placa\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Veículo/Modelo\n        \"mft_vie_model\": {\n            //Nome do campo\n            \"title\": \"Veículo/Modelo\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Veículo/Cor\n        \"mft_vie_color\": {\n            //Nome do campo\n            \"title\": \"Veículo/Cor\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Pessoa/Nome Fantasia\n        \"mft_crn_psn_nickname\": {\n            //Nome do campo\n            \"title\": \"Pessoa/Nome Fantasia\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Manifesto/Adiantamento\n        \"mft_mts_mft_advance_subtotal\": {\n            //Nome do campo\n            \"title\": \"Manifesto/Adiantamento\",\n            //Tipo de valor que o campo retorna\n            \"filter\": \"decimal\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Número\n        \"sequence_code\": {\n            //Nome do campo\n            \"title\": \"Número\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": true\n        },\n        //Dados da coluna Manifesto/Custo total\n        \"mft_cat_mft_total_cost\": {\n            //Nome do campo\n            \"title\": \"Manifesto/Custo total\",\n            //Tipo de valor que o campo retorna\n            \"filter\": \"decimal\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Frete/N°\n        \"mft_mts_fit_sequence_code\": {\n            //Nome do campo\n            \"title\": \"Frete/N°\",\n            //Tipo de valor que o campo retorna\n            \"filter\": \"integer\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        },\n        //Dados da coluna Manifesto/Número\n        \"mft_mts_mft_sequence_code\": {\n            //Nome do campo\n            \"title\": \"Manifesto/Número\",\n            //Tamanho máximo do campo\n            \"max_width\": 150,\n            //\n            \"order\": false\n        }\n    },\n    //Filtros do template - Os dados que retornam, podem variar de acordo com a estrutura do template criado.\n    \"filters\": [\n        {\n            //Nome do filtro \n            \"label\": \"Data\",\n            //Posição do filtro \n            \"position\": 1,\n            //Campo que irá realizar o filtro\n            \"field\": \"service_date\",\n            //Tabela na qual irá consutar os dados\n            \"table\": \"manifests\",\n            //Tipo de dado que deve ser passado\n            \"type\": \"date\",\n            //\n            \"select\": null,\n            //Tamanho máximo que pode ser passado no filtro\n            \"maxlength\": null,\n            //\n            \"collection\": null\n        },\n        {\n            //Nome do filtro \n            \"label\": \"Número\",\n            //Posição do filtro \n            \"position\": 2,\n            //Campo que irá realizar o filtro\n            \"field\": \"sequence_code\",\n            //Tabela na qual irá consutar os dados\n            \"table\": \"manifests\",\n            //Tipo de dado que deve ser passado\n            \"type\": \"numeric\",\n            //\n            \"select\": null,\n            //Tamanho máximo que pode ser passado no filtro\n            \"maxlength\": null,\n            //\n            \"collection\": null\n        }\n    ],\n    //Ordem de predefinição\n    \"default_order\": \"service_date\",\n    //Filtro de data requerido\n    \"required_date_filters\": [\n        \"manifests.service_date\"\n    ]\n}"
						}
					]
				},
				{
					"name": "3) Executa consulta relatório",
					"protocolProfileBehavior": {
						"disableBodyPruning": true
					},
					"request": {
						"auth": {
							"type": "bearer",
							"bearer": [
								{
									"key": "token",
									"value": "***TOKEN_CENSURADO_POR_SEGURANCA***",
									"type": "string"
								}
							]
						},
						"method": "GET",
						"header": [],
						"body": {
							"mode": "raw",
							"raw": "{\r\n    //Procurar\r\n    \"search\": {\r\n        //Na requisição anterior existe um array com os filtros (filters), onde devem ser retiradas as informações para informar o filtro nesta requisição\r\n        //Nome da tabela (filters > table)\r\n        \"manifests\": {\r\n            //Nome do campo (filters > field)\r\n            \"service_date\": \"2025-10-01 - 2025-10-05\"\r\n        }\r\n    },\r\n    //Número de páginas\r\n    \"page\": \"1\",\r\n    //Número de retornos da consulta que é  possível visualizar por página\r\n    \"per\": \"100\"\r\n}",
							"options": {
								"raw": {
									"language": "json"
								}
							}
						},
						"url": {
							"raw": "https://rodogarcia.eslcloud.com.br/api/analytics/reports/6399/data",
							"protocol": "https",
							"host": [
								"rodogarcia",
								"eslcloud",
								"com",
								"br"
							],
							"path": [
								"api",
								"analytics",
								"reports",
								"6399",
								"data"
							]
						},
						"description": "A requisição para realizar a **consulta dos relatórios do Data Export**, deve ser feita **utilizando o método GET** e especificando no endereço o _tenant_ correspondente, para realizar a consulta é **necessário passar como parâmetro o ID do template** que deseja buscar, **na URL**, esse **ID é encontrado no retorno da consulta dos templates**.\n\n[Observações:](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#0ae0ce29-746a-4fd7-ba70-9f2d5ea432f0)\n\n- Todas as requisições devem respeitar um intervalo de 2 segundos entre elas, se utilizado o mesmo endereço IP. Caso contrário, o status code 429 Too Many Requests será retornado;\n    \n    - Para diferentes IP’s não é necessário o intervalo de 2 segundos;\n        \n    - Se no período consultado a data for inferior a 31 dias, o sistema permite solicitar quantas vezes for necessário sem impor limites de horas, porém, seguindo a regra citada anteriormente;\n        \n    - Se no período consultado a data estiver entre 31 dias e 6 meses, o sistema limitará por 1 hora a nova extração de dados;\n        \n    - Se no período consultado a data estiver acima de 6 meses, o sistema limitará por 12 horas a nova extração de dados.\n        \n- **Token**: Para utilização da API é necessário gerar o Token, e com a finalidade de facilitar a utilização das nossas ferramentas temos uma Wiki, nela é possível encontrar detalhes de como gerar o [Wiki token API.](https://eslcloud.zendesk.com/hc/pt-br/articles/20492545919885--Gera%C3%A7%C3%A3o-Token-Token-API-Cadastro-Token-API)\n    \n- **endPoint:** Alterar a palavra subdominio para o ([<i>Tenant</i>](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#6cc933c1-294b-46d8-99a8-d581ea8f6bc6)) da transportadora.\n    \n\n_Atualizado 08/07/2024_"
					},
					"response": [
						{
							"name": "3) Retorno - Executa consulta relatório",
							"originalRequest": {
								"method": "GET",
								"header": [],
								"body": {
									"mode": "raw",
									"raw": "{\t\r\n\t\"search\": {\r\n\t\t\"manifests\": { \r\n\t\t\t\"service_date\": \"2021-10-05 - 2021-10-05\"\r\n\t\t}\r\n\t},\r\n    \"page\": \"1\",\r\n    \"per\": \"100\"\r\n}\r\n",
									"options": {
										"raw": {
											"language": "json"
										}
									}
								},
								"url": {
									"raw": "http://beta1.tmsweb.com.br/api/analytics/reports/3/data",
									"protocol": "http",
									"host": [
										"beta1",
										"tmsweb",
										"com",
										"br"
									],
									"path": [
										"api",
										"analytics",
										"reports",
										"3",
										"data"
									]
								}
							},
							"status": "OK",
							"code": 200,
							"_postman_previewlanguage": "json",
							"header": [
								{
									"key": "Server",
									"value": "Cowboy"
								},
								{
									"key": "Date",
									"value": "Mon, 10 Oct 2022 17:55:04 GMT"
								},
								{
									"key": "Connection",
									"value": "keep-alive"
								},
								{
									"key": "X-Frame-Options",
									"value": "SAMEORIGIN"
								},
								{
									"key": "X-Xss-Protection",
									"value": "1; mode=block"
								},
								{
									"key": "X-Content-Type-Options",
									"value": "nosniff"
								},
								{
									"key": "X-Download-Options",
									"value": "noopen"
								},
								{
									"key": "X-Permitted-Cross-Domain-Policies",
									"value": "none"
								},
								{
									"key": "Referrer-Policy",
									"value": "strict-origin-when-cross-origin"
								},
								{
									"key": "Content-Type",
									"value": "application/json; charset=utf-8"
								},
								{
									"key": "Vary",
									"value": "Accept-Encoding, Origin"
								},
								{
									"key": "Content-Encoding",
									"value": "gzip"
								},
								{
									"key": "Etag",
									"value": "W/\"4b0082d6e615b18b0ace21d24a895bb4\""
								},
								{
									"key": "Cache-Control",
									"value": "max-age=0, private, must-revalidate"
								},
								{
									"key": "X-Meta-Request-Version",
									"value": "0.7.2"
								},
								{
									"key": "X-Request-Id",
									"value": "220e953c-266f-4032-8c43-63cf2326e9fe"
								},
								{
									"key": "X-Runtime",
									"value": "0.171043"
								},
								{
									"key": "Transfer-Encoding",
									"value": "chunked"
								},
								{
									"key": "Via",
									"value": "1.1 vegur"
								}
							],
							"cookie": [],
							"body": "[\n    //Esse retorno da API apresenta todos os dados do RELATÓRIO criado dentro do template que foi especificado na URL. Cada bloco de dados passados entre chaves, representa uma linha dos dados que retornaram da consulta estruturada no template criado.\n    //Os nomes e valores que retornar podem variar de acordo com a estrutura do template criado pelo usuário, já que o mesmo tem a opção de edita-lo a sua própria conveniência.\n    \n    //Abaixo é apresentado um exemplo de consulta para que seja possível compreender a estrutura do resultado esperado.\n    {\n        //Dados que retornaram na primeira linha do relatório:\n\n        //Valor da coluna ID do manifesto\n        \"id\": 296389,\n        //Valor da coluna Número do manifesto\n        \"sequence_code\": 22699,\n        //Valor da coluna custo total do manifesto\n        \"mft_cat_mft_total_cost\": null,\n        //Valor da coluna ID da Filial de Origem\n        \"mft_crn_id\": 73080,\n        //Valor da coluna Nome da Filial de Origem\n        \"mft_crn_psn_name\": \"ESL CONSULTORIA E SERVICOS EM INFORMATICA LTDA\",\n        //Valor da coluna Nome Fantasia da Filial de origem\n        \"mft_crn_psn_nickname\": \"ESL FILIAL\",\n        //Valor da coluna Motorista\n        \"mft_mdr_iil_name\": \"Caroline Batista Vantim\",\n        //Valor da coluna Número do Frete\n        \"mft_mts_fit_sequence_code\": 643200,\n        //Valor da coluna Manifesto/Adiantamento\n        \"mft_mts_mft_advance_subtotal\": \"0.0\",\n        //Valor da culuna Número do Manifesto\n        \"mft_mts_mft_sequence_code\": 22699,\n        //Valor da coluna Veículo/Placa\n        \"mft_vie_license_plate\": \"QZD7C29\",\n        //Valor da coluna Veículo/Modelo\n        \"mft_vie_model\": \"2018\",\n        //Valor da coluna Veículo/Cor\n        \"mft_vie_color\": \"BRANCA\"\n    },\n    {\n        //Valor da coluna ID do manifesto\n        \"id\": 296389,\n        //Valor da coluna Número do manifesto\n        \"sequence_code\": 22699,\n        //Valor da coluna custo total do manifesto\n        \"mft_cat_mft_total_cost\": null,\n        //Valor da coluna ID da Filial de Origem\n        \"mft_crn_id\": 73080,\n        //Valor da coluna Nome da Filial de Origem\n        \"mft_crn_psn_name\": \"ESL CONSULTORIA E SERVICOS EM INFORMATICA LTDA\",\n        //Valor da coluna Nome Fantasia da Filial de origem\n        \"mft_crn_psn_nickname\": \"ESL FILIAL\",\n        //Valor da coluna Motorista\n        \"mft_mdr_iil_name\": \"Caroline Batista Vantim\",\n        //Valor da coluna Número do Frete\n        \"mft_mts_fit_sequence_code\": null,\n        //Valor da coluna Manifesto/Adiantamento    \n        \"mft_mts_mft_advance_subtotal\": \"0.0\",\n        //Valor da culuna Número do Manifesto\n        \"mft_mts_mft_sequence_code\": 22699,\n        //Valor da coluna Veículo/Placa\n        \"mft_vie_license_plate\": \"QZD7C29\",\n        //Valor da coluna Veículo/Modelo\n        \"mft_vie_model\": \"2018\",\n        //Valor da coluna Veículo/Cor\n        \"mft_vie_color\": \"BRANCA\"\n    },\n    {\n        //Valor da coluna ID do manifesto\n        \"id\": 296389,\n        //Valor da coluna Número do manifesto\n        \"sequence_code\": 22699,\n        //Valor da coluna custo total do manifesto\n        \"mft_cat_mft_total_cost\": null,\n        //Valor da coluna ID da Filial de Origem\n        \"mft_crn_id\": 73080,\n        //Valor da coluna Nome da Filial de Origem\n        \"mft_crn_psn_name\": \"ESL CONSULTORIA E SERVICOS EM INFORMATICA LTDA\",\n        //Valor da coluna Nome Fantasia da Filial de origem\n        \"mft_crn_psn_nickname\": \"ESL FILIAL\",\n        //Valor da coluna Motorista\n        \"mft_mdr_iil_name\": \"Caroline Batista Vantim\",\n        //Valor da coluna Número do Frete\n        \"mft_mts_fit_sequence_code\": 12362,\n        //Valor da coluna Manifesto/Adiantamento\n        \"mft_mts_mft_advance_subtotal\": \"0.0\",\n        //Valor da culuna Número do Manifesto\n        \"mft_mts_mft_sequence_code\": 22699,\n        //Valor da coluna Veículo/Placa\n        \"mft_vie_license_plate\": \"QZD7C29\",\n        //Valor da coluna Veículo/Modelo\n        \"mft_vie_model\": \"2018\",\n        //Valor da coluna Veículo/Cor\n        \"mft_vie_color\": \"BRANCA\"\n    },\n    {\n        //Valor da coluna ID do manifesto\n        \"id\": 296387,\n        //Valor da coluna Número do manifesto\n        \"sequence_code\": 22697,\n        //Valor da coluna custo total do manifesto\n        \"mft_cat_mft_total_cost\": null,\n        //Valor da coluna ID da Filial de Origem\n        \"mft_crn_id\": 73080,\n        //Valor da coluna Nome da Filial de Origem\n        \"mft_crn_psn_name\": \"ESL CONSULTORIA E SERVICOS EM INFORMATICA LTDA\",\n        //Valor da coluna Nome Fantasia da Filial de origem\n        \"mft_crn_psn_nickname\": \"ESL FILIAL\",\n        //Valor da coluna Motorista\n        \"mft_mdr_iil_name\": \"Pedro Físico\",\n        //Valor da coluna Número do Frete\n        \"mft_mts_fit_sequence_code\": 641871,\n        //Valor da coluna Manifesto/Adiantamento\n        \"mft_mts_mft_advance_subtotal\": \"0.0\",\n        //Valor da culuna Número do Manifesto\n        \"mft_mts_mft_sequence_code\": 22697,\n        //Valor da coluna Veículo/Placa\n        \"mft_vie_license_plate\": \"OAB4904\",\n        //Valor da coluna Veículo/Modelo\n        \"mft_vie_model\": \"FORD CARGO \",\n        //Valor da coluna Veículo/Cor\n        \"mft_vie_color\": \"BRANCA\"\n    }\n]"
						}
					]
				},
				{
					"name": "4) Consulta do relatório e solicitação de exportação em XLSX para FTP",
					"request": {
						"auth": {
							"type": "bearer",
							"bearer": [
								{
									"key": "token",
									"value": "***TOKEN_CENSURADO_POR_SEGURANCA***",
									"type": "string"
								}
							]
						},
						"method": "POST",
						"header": [],
						"body": {
							"mode": "raw",
							"raw": "{\t\r\n    //Para exportar o FTP é necessário indicar o valor true no campo export_to_ftp, o valor false só será usado em caso de consultas onde não será realizada a exportação o FTP.\r\n    \"export_to_ftp\":false,\r\n    //Procurar\r\n\t\"search\": {\r\n        //Nome da tabela (filters > table)\r\n\t\t\"manifests\": { \r\n            //Nome do campo (filters > field) no qual será realizado a pesquisa\r\n\t\t\t\"service_date\": \"2025-10-05 - 2025-10-05\"\r\n\t\t}\r\n\t}\r\n}\r\n",
							"options": {
								"raw": {
									"language": "json"
								}
							}
						},
						"url": {
							"raw": "https://rodogarcia.eslcloud.com.br/api/analytics/reports/6399/export",
							"protocol": "https",
							"host": [
								"rodogarcia",
								"eslcloud",
								"com",
								"br"
							],
							"path": [
								"api",
								"analytics",
								"reports",
								"6399",
								"export"
							]
						},
						"description": "A requisição para realizar a **consulta do relatório e solicitação de exportação em XLSX para FTP do Data Export**, deve ser feita **utilizando o método POST** e especificando no endereço o _tenant_ correspondente, para realizar a consulta é **necessário passar como parâmetro o ID do template**, **na URL**, esse **ID é encontrado no retorno da consulta dos templates.**\n\n[Observações:](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#0ae0ce29-746a-4fd7-ba70-9f2d5ea432f0)\n\n- Todas as requisições devem respeitar um intervalo de 2 segundos entre elas, se utilizado o mesmo endereço IP. Caso contrário, o status code 429 Too Many Requests será retornado;\n    \n    - Para diferentes IP’s não é necessário o intervalo de 2 segundos;\n        \n    - Se no período consultado a data for inferior a 31 dias, o sistema permite solicitar quantas vezes for necessário sem impor limites de horas, porém, seguindo a regra citada anteriormente;\n        \n    - Se no período consultado a data estiver entre 31 dias e 6 meses, o sistema limitará por 1 hora a nova extração de dados;\n        \n    - Se no período consultado a data estiver acima de 6 meses, o sistema limitará por 12 horas a nova extração de dados.\n        \n- **Token**: Para utilização da API é necessário gerar o Token, e com a finalidade de facilitar a utilização das nossas ferramentas temos uma Wiki, nela é possível encontrar detalhes de como gerar o [Wiki token API.](https://eslcloud.zendesk.com/hc/pt-br/articles/20492545919885--Gera%C3%A7%C3%A3o-Token-Token-API-Cadastro-Token-API)\n    \n- **endPoint:** Alterar a palavra subdominio para o ([<i>Tenant</i>](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#6cc933c1-294b-46d8-99a8-d581ea8f6bc6)) da transportadora.\n    \n\n_Atualizado 08/07/2024_"
					},
					"response": [
						{
							"name": "4) Retorno - Consulta do relatório e solicitação de exportação em XLSX para FTP",
							"originalRequest": {
								"method": "POST",
								"header": [],
								"body": {
									"mode": "raw",
									"raw": "{\t\r\n    \"export_to_ftp\":true,\r\n\t\"search\": {\r\n\t\t\"manifests\": { \r\n\t\t\t\"service_date\": \"2021-10-05 - 2021-10-05\"\r\n\t\t}\r\n\t}\r\n}\r\n",
									"options": {
										"raw": {
											"language": "json"
										}
									}
								},
								"url": {
									"raw": "http://beta1.tmsweb.com.br/api/analytics/reports/3/export",
									"protocol": "http",
									"host": [
										"beta1",
										"tmsweb",
										"com",
										"br"
									],
									"path": [
										"api",
										"analytics",
										"reports",
										"3",
										"export"
									]
								}
							},
							"status": "OK",
							"code": 200,
							"_postman_previewlanguage": "json",
							"header": [
								{
									"key": "Server",
									"value": "Cowboy"
								},
								{
									"key": "Date",
									"value": "Mon, 10 Oct 2022 17:55:34 GMT"
								},
								{
									"key": "Connection",
									"value": "keep-alive"
								},
								{
									"key": "X-Frame-Options",
									"value": "SAMEORIGIN"
								},
								{
									"key": "X-Xss-Protection",
									"value": "1; mode=block"
								},
								{
									"key": "X-Content-Type-Options",
									"value": "nosniff"
								},
								{
									"key": "X-Download-Options",
									"value": "noopen"
								},
								{
									"key": "X-Permitted-Cross-Domain-Policies",
									"value": "none"
								},
								{
									"key": "Referrer-Policy",
									"value": "strict-origin-when-cross-origin"
								},
								{
									"key": "Content-Type",
									"value": "application/json; charset=utf-8"
								},
								{
									"key": "Vary",
									"value": "Accept-Encoding, Origin"
								},
								{
									"key": "Content-Encoding",
									"value": "gzip"
								},
								{
									"key": "Etag",
									"value": "W/\"62b57c57ad36e1f115301947c05f337d\""
								},
								{
									"key": "Cache-Control",
									"value": "max-age=0, private, must-revalidate"
								},
								{
									"key": "X-Meta-Request-Version",
									"value": "0.7.2"
								},
								{
									"key": "X-Request-Id",
									"value": "93570402-bd2d-4e1f-b55f-4bcbb7ac7d02"
								},
								{
									"key": "X-Runtime",
									"value": "0.136103"
								},
								{
									"key": "Transfer-Encoding",
									"value": "chunked"
								},
								{
									"key": "Via",
									"value": "1.1 vegur"
								}
							],
							"cookie": [],
							"body": "//O retorno dessa API apresenta os dados do arquivo que será gerado.\n\n//Os valores dos campos que retornam, podem variar de acordo com a estrutura do template criado pelo usuário, já que o mesmo tem a opção de edita-lo a sua própria conveniência.\n    \n//Abaixo é apresentado um exemplo de consulta para que seja possível compreender a estrutura do resultado esperado.\n{\n    //ID do Relatório gerado\n    \"id\": 206,\n    //ID do tenant\n    \"esl_tenant_id\": 68,\n    //ID do template\n    \"report_id\": 3,\n    //ID do usuário que gerou o arquivo\n    \"user_id\": 11553,\n    //Estatus do processamento do arquivo\n    \"status\": \"enqueued\",\n    //Filtros passados no template\n    \"filters\": {\n        //Tabela na qual será filtrado\n        \"manifests\": {\n            //Campo e valor a ser filtrado.\n            \"service_date\": \"20/09/2022 - 20/09/2022\"\n        }\n    },\n    //Quantidade de arquivos processados\n    \"processed\": 0,\n    //Data em que o relatório foi gerado\n    \"created_at\": \"2022-10-10T14:55:35.118-03:00\",\n    //Data da última atualização do relatório\n    \"updated_at\": \"2022-10-10T14:55:35.138-03:00\",\n    //Verificação de se foi exportado ou não\n    \"export\": true,\n    //Formatação do Status no caso o nome do status disponível para visualização muda de enqueued para agendado\n    \"status_formatted\": \"Agendado\",\n    //Nome do arquivo que foi gerado\n    \"filename\": \"teste-manifesto_2022_10_10_14_55\",\n    //Nome do template\n    \"report_name\": \"Teste Manifesto\",\n    //URL da API\n    \"show_url\": \"/api/analytics/report_files/206\",\n    //URL do arquivo gerado, a mesma, quando inserida no navegador, realiza o download do arquivo no armazenamento local da máquina.\n    \"download_url\": null\n}"
						}
					]
				},
				{
					"name": "5) Consulta do arquivo gerado",
					"request": {
						"auth": {
							"type": "bearer",
							"bearer": [
								{
									"key": "token",
									"value": "***TOKEN_CENSURADO_POR_SEGURANCA***",
									"type": "string"
								}
							]
						},
						"method": "GET",
						"header": [],
						"url": {
							"raw": "https://rodogarcia.eslcloud.com.br/api/analytics/report_files/920679",
							"protocol": "https",
							"host": [
								"rodogarcia",
								"eslcloud",
								"com",
								"br"
							],
							"path": [
								"api",
								"analytics",
								"report_files",
								"920679"
							]
						},
						"description": "A requisição para realizar a **consulta do arquivo gerado**, deve ser feita **utilizando o método GET** e especificando no endereço o _tenant_ correspondente, para realizar a consulta é **necessário passar o campo show_url como parâmetro na URL da API,** que é **encontrado no retorno da consulta do relatório e solicitação de exportação em XLSX para FTP.**\n\n[Observações:](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#0ae0ce29-746a-4fd7-ba70-9f2d5ea432f0)\n\n- Todas as requisições devem respeitar um intervalo de 2 segundos entre elas, se utilizado o mesmo endereço IP. Caso contrário, o status code 429 Too Many Requests será retornado;\n    \n    - Para diferentes IP’s não é necessário o intervalo de 2 segundos;\n        \n    - Se no período consultado a data for inferior a 31 dias, o sistema permite solicitar quantas vezes for necessário sem impor limites de horas, porém, seguindo a regra citada anteriormente;\n        \n    - Se no período consultado a data estiver entre 31 dias e 6 meses, o sistema limitará por 1 hora a nova extração de dados;\n        \n    - Se no período consultado a data estiver acima de 6 meses, o sistema limitará por 12 horas a nova extração de dados.\n        \n- **Token**: Para utilização da API é necessário gerar o Token, e com a finalidade de facilitar a utilização das nossas ferramentas temos uma Wiki, nela é possível encontrar detalhes de como gerar o [Wiki token API.](https://eslcloud.zendesk.com/hc/pt-br/articles/20492545919885--Gera%C3%A7%C3%A3o-Token-Token-API-Cadastro-Token-API)\n    \n- **endPoint:** Alterar a palavra subdominio para o ([<i>Tenant</i>](https://documenter.getpostman.com/view/20571375/2s9YXk2fj5#6cc933c1-294b-46d8-99a8-d581ea8f6bc6)) da transportadora.\n    \n\n_Atualizado 08/07/2024_"
					},
					"response": [
						{
							"name": "5) Retorno - Consulta do arquivo gerado",
							"originalRequest": {
								"method": "GET",
								"header": [],
								"url": {
									"raw": "http://beta1.tmsweb.com.br/api/analytics/report_files/164",
									"protocol": "http",
									"host": [
										"beta1",
										"tmsweb",
										"com",
										"br"
									],
									"path": [
										"api",
										"analytics",
										"report_files",
										"164"
									]
								}
							},
							"status": "OK",
							"code": 200,
							"_postman_previewlanguage": "json",
							"header": [
								{
									"key": "Server",
									"value": "Cowboy"
								},
								{
									"key": "Date",
									"value": "Mon, 10 Oct 2022 17:56:07 GMT"
								},
								{
									"key": "Connection",
									"value": "keep-alive"
								},
								{
									"key": "X-Frame-Options",
									"value": "SAMEORIGIN"
								},
								{
									"key": "X-Xss-Protection",
									"value": "1; mode=block"
								},
								{
									"key": "X-Content-Type-Options",
									"value": "nosniff"
								},
								{
									"key": "X-Download-Options",
									"value": "noopen"
								},
								{
									"key": "X-Permitted-Cross-Domain-Policies",
									"value": "none"
								},
								{
									"key": "Referrer-Policy",
									"value": "strict-origin-when-cross-origin"
								},
								{
									"key": "Content-Type",
									"value": "application/json; charset=utf-8"
								},
								{
									"key": "Vary",
									"value": "Accept-Encoding, Origin"
								},
								{
									"key": "Content-Encoding",
									"value": "gzip"
								},
								{
									"key": "Etag",
									"value": "W/\"504971d5c06dd0fbedc647c455f04d55\""
								},
								{
									"key": "Cache-Control",
									"value": "max-age=0, private, must-revalidate"
								},
								{
									"key": "X-Meta-Request-Version",
									"value": "0.7.2"
								},
								{
									"key": "X-Request-Id",
									"value": "a195a9c4-2cf4-4346-b97f-78b10ea8c75b"
								},
								{
									"key": "X-Runtime",
									"value": "0.096383"
								},
								{
									"key": "Transfer-Encoding",
									"value": "chunked"
								},
								{
									"key": "Via",
									"value": "1.1 vegur"
								}
							],
							"cookie": [],
							"body": "{\n    //O retorno dessa API apresenta os dados do arquivo que foi gerado na API: 4) Consulta do relatório e solicitação de exportação em XLSX para FTP. \n    //Os valores que retornar podem variar de acordo com a estrutura do template criado pelo usuário, já que o mesmo tem a opção de edita-lo a sua própria conveniência.\n    \n    //Abaixo é apresentado um exemplo de consulta para que seja possível compreender a estrutura do resultado esperado.\n\n    //ID do arquivo gerado\n    \"id\": 164,\n    //ID do tenant\n    \"esl_tenant_id\": 68,\n    //ID do template\n    \"report_id\": 3,\n    //ID do usuário que gerou o arquivo\n    \"user_id\": 11553,\n    //Status do arquivo, se está em processamento ou se ja foi gerado\n    \"status\": \"generated\",\n    //Filtros passados no template\n    \"filters\": {\n        //Tabela na qual será filtrado\n        \"manifests\": {\n            //Campo e valor a ser filtrado.\n            \"service_date\": \"29/01/2022 - 29/04/2022\"\n        }\n    },\n    //\n    \"processed\": 152,\n    //Data da criação do arquivo\n    \"created_at\": \"2022-07-19T13:47:52.929-03:00\",\n    //Data da ultima atualização do arquivo\n    \"updated_at\": \"2022-07-19T13:48:02.474-03:00\",\n    //Verificação de se foi exportado ou não\n    \"export\": true,\n    //Formatação do Status no caso o nome do status disponível para visualização muda de generated para gerado\n    \"status_formatted\": \"Gerado\",\n    //Nome do arquivo que foi gerado\n    \"filename\": \"teste-manifesto_2022_07_19_13_47\",\n    //Nome do template\n    \"report_name\": \"Teste Manifesto\",\n    //URL da API\n    \"show_url\": \"/api/analytics/report_files/164\",\n    //URL do arquivo gerado, a mesma, quando inserida no navegador, realiza o download do arquivo no armazenamento local da máquina.\n    \"download_url\": \"https://tmsdev-beta.s3.sa-east-1.amazonaws.com/beta1/active_storage/xUYsKEohtoPD7bv6ogL8cCv8?response-content-disposition=attachment%3B%20filename%3D%22teste-manifesto_2022_07_19_13_47.xlsx%22%3B%20filename%2A%3DUTF-8%27%27teste-manifesto_2022_07_19_13_47.xlsx&response-content-type=application%2Fvnd.openxmlformats-officedocument.spreadsheetml.sheet&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=***CREDENCIAL_AWS_CENSURADA_POR_SEGURANCA***%2F20221010%2Fsa-east-1%2Fs3%2Faws4_request&X-Amz-Date=20221010T175608Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=0255a4a2e074ebf4b0ce22b4542257c2c9708d1908ef46492a4546eb4b343adb\"\n}"
						}
					]
				}
			],
			"description": "A seguir serão apresentadas as requests relacionadas ao Data Export, esta API permite a consulta dos templates e filtros do data export, com essas informações é possível extrair o relatório em JSON ou exportar um arquivo XLSX para um FTP previamente cadastrado, como se trata de uma API específica do sistema, para autorizar o acesso, é necessário gerar um token dentro do cadastro de usuários para autenticar essa API.\n\n**Token**: Para obter o Token de acesso desta API, basta acessar o seguinte caminho na tela do TMS: **Cadastros > Usuários > Aba API > Editar usuário > Gerar token**\n\nPara mais detalhes de como gerar este Token é possível acessar nossa Wiki pelo link: [Wiki token API.](https://eslcloud.zendesk.com/hc/pt-br/articles/20492545919885--Gera%C3%A7%C3%A3o-Token-Token-API-Cadastro-Token-API)\n\n**Observações:** Todas as requisições devem respeitar um intervalo de 2 segundos entre elas, se utilizado o mesmo endereço IP. Caso contrário, o status code 429 Too Many Requests será retornado;\n\nPara diferentes IP’s não é necessário o intervalo de 2 segundos;\n\n**Para consultas realizadas através do JSON, devem ser consideradas as seguintes regras:**\n\n- Se no período consultado a data for inferior a 31 dias, o sistema permite solicitar quantas vezes for necessário sem impor limites de horas, porém, seguindo a regra citada anteriormente;\n    \n- Se no período consultado a data estiver entre 31 dias e 6 meses, o sistema limitará por 1 hora a nova extração de dados;\n    \n- Se no período consultado a data estiver acima de 6 meses, o sistema limitará por 12 horas a nova extração de dados.\n    \n\n**Para consultas realizadas através do arquivo .XLSX, devem ser consideradas as seguintes regras:**\n\n- **Período até 7 dias:**  \n    Não há reaproveitamento de arquivos (cache). O relatório é sempre gerado do zero.\n    \n- **Período superior a 7 dias:**  \n    O sistema tenta encontrar um relatório similar, com os mesmos filtros, criado na última 1 hora.  \n    Se encontrado, esse arquivo é reaproveitado e vinculado ao novo relatório.\n    \n- **Período superior a 6 meses:**  \n    Caso não haja reaproveitamento na tentativa anterior, o sistema realiza uma nova busca por arquivos similares com status :generated (Gerado), criados nas últimas 12 horas.\n    \n\n**Subdomínio** _**(tenant)**_ Na URL(endPoint) a palavra \"**subdomínio**\" deve ser trocada pelo nome de endereço de acesso da transportadora, como por exemplo: https://**transportadoraexemplo**.escloud.com.br/",
			"auth": {
				"type": "bearer",
				"bearer": [
					{
						"key": "token",
						"value": "***TOKEN_CENSURADO_POR_SEGURANCA***",
						"type": "string"
					}
				]
			},
			"event": [
				{
					"listen": "prerequest",
					"script": {
						"type": "text/javascript",
						"exec": [
							""
						]
					}
				},
				{
					"listen": "test",
					"script": {
						"type": "text/javascript",
						"exec": [
							""
						]
					}
				}
			]
		}
	]
}