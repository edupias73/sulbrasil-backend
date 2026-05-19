-- Massa de dados para testes do catálogo (linha pesada + leve)
DELETE FROM aplicacoes_veiculo;
DELETE FROM codigos_cruzados;
DELETE FROM produtos;

INSERT INTO produtos (id, codigo_interno, nome_peca, marca_principal, preco, quantidade_estoque, termos_busca) VALUES
(1, 'SB-FO-SCAN440', 'Filtro de Oleo Lubrificante Motor', 'ZEN',
 89.90, 24,
 'filtro de oleo lubrificante motor zen zen fo-1234 mann hu12105x w940/18 scania r440 r450 r series'),
(2, 'SB-PF-ACTROS', 'Pastilha de Freio Dianteira', 'ZM',
 245.00, 12,
 'pastilha de freio dianteira zm zm pf-8870 wabco 29174 mercedes actros mp4 mp5'),
(3, 'SB-DIS-VOLFH', 'Disco de Freio Ventilado Traseiro', 'ZEN',
 520.50, 8,
 'disco de freio ventilado traseiro zen zen df-5521 textar 984002430 volvo fh fh 460 fh 500'),
(4, 'SB-REL-BMW32', 'Rele Auxiliar de Partida', 'Bosch',
 78.40, 35,
 'rele auxiliar de partida bosch bosch 0 332 209 150 siemens v23079-b2201-b301 bmw serie 3 e90 e91'),
(5, 'SB-REL-BMW12', 'Rele de Bomba de Combustivel', 'Valeo',
 62.90, 28,
 'rele de bomba de combustivel valeo valeo 409 502 bmw serie 1 serie 3'),
(6, 'SB-SEN-GOLG5', 'Sensor ABS Roda Dianteira', 'Cofap',
 95.00, 40,
 'sensor abs roda dianteira cofap cofap scg101 bosch 0265006572 volkswagen gol voyage g5 g6');

INSERT INTO codigos_cruzados (produto_id, marca_fabricante, codigo) VALUES
(1, 'ZEN', 'FO-1234'),
(1, 'MANN', 'HU12105X'),
(1, 'MANN', 'W940/18'),
(2, 'ZM', 'PF-8870'),
(2, 'WABCO', '29174'),
(3, 'ZEN', 'DF-5521'),
(3, 'TEXTAR', '984002430'),
(4, 'BOSCH', '0 332 209 150'),
(4, 'SIEMENS', 'V23079-B2201-B301'),
(5, 'VALEO', '409 502'),
(6, 'COFAP', 'SCG101'),
(6, 'BOSCH', '0265006572');

INSERT INTO aplicacoes_veiculo (produto_id, montadora, veiculo, ano_inicio, ano_fim) VALUES
(1, 'Scania', 'R440 / R450 R-Series', 2010, 2022),
(1, 'Scania', 'G440 / G450', 2012, 2020),
(2, 'Mercedes-Benz', 'Actros MP4', 2014, 2024),
(2, 'Mercedes-Benz', 'Actros MP5', 2020, 2026),
(3, 'Volvo', 'FH 460 / FH 500', 2013, 2023),
(4, 'BMW', 'Serie 3 E90 / E91', 2005, 2012),
(4, 'BMW', 'Serie 3 F30', 2012, 2018),
(5, 'BMW', 'Serie 1 E87', 2007, 2013),
(5, 'BMW', 'Serie 3 E90', 2005, 2011),
(6, 'Volkswagen', 'Gol G5', 2008, 2012),
(6, 'Volkswagen', 'Voyage G5', 2009, 2013);
