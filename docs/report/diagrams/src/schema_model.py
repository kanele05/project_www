# -*- coding: utf-8 -*-
"""Đọc database/02_schema.sql thành mô hình (bảng, cột, kiểu, NULL, PK, FK) để vẽ ERD và Database Diagram.
Nguồn sự thật duy nhất là tệp SQL: sơ đồ không bao giờ lệch khỏi lược đồ thật."""
import os, re

SQL = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', '..', 'database', '02_schema.sql')


def _type(t):
    t = t.strip().lower()
    return t.replace('(max)', '(max)')


def load(path=SQL):
    sql = open(path, encoding='utf8').read()
    tables = {}      # tên -> {'cols': [ {name,type,null,pk,fk,uk} ], 'order': n}
    for m in re.finditer(r'CREATE TABLE (\w+) \((.*?)\n\);', sql, re.S):
        name, body = m.group(1), m.group(2)
        cols = []
        for line in body.split('\n'):
            c = re.match(r'\s+(\w+)\s+(BIGINT|NVARCHAR\(\w+\)|INT|BIT|DATETIME2\(\d\)|DATE|NUMERIC\(\d+,\d+\))'
                         r'(?:\s+IDENTITY\(1,1\))?\s+(NOT NULL|NULL)', line)
            if c:
                cols.append({'name': c.group(1), 'type': _type(c.group(2)).replace(',', ', '),
                             'null': c.group(3) == 'NULL', 'pk': False, 'fk': False, 'unique': False})
        tables[name] = {'name': name, 'cols': cols, 'fks': []}
    # PK, FK, UNIQUE đơn cột (trong CREATE TABLE)
    for m in re.finditer(r'CREATE TABLE (\w+) \((.*?)\n\);', sql, re.S):
        t = tables[m.group(1)]
        body = m.group(2)
        for pk in re.findall(r'PRIMARY KEY \((\w+)\)', body):
            _col(t, pk)['pk'] = True
        for uk in re.findall(r'UNIQUE \((\w+)\)', body):
            _col(t, uk)['unique'] = True
        for fkname, col, ref in re.findall(r'CONSTRAINT (\w+) FOREIGN KEY \((\w+)\)\s+REFERENCES (\w+) \(id\)', body):
            t['fks'].append({'name': fkname, 'col': col, 'ref': ref})
    # ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY
    for tbl, fkname, col, ref in re.findall(
            r'ALTER TABLE (\w+)\s+ADD CONSTRAINT (\w+) FOREIGN KEY \((\w+)\)\s+REFERENCES (\w+) \(id\)', sql):
        tables[tbl]['fks'].append({'name': fkname, 'col': col, 'ref': ref})
    for t in tables.values():
        for fk in t['fks']:
            c = _col(t, fk['col'])
            c['fk'] = True
            fk['null'] = c['null']
            fk['unique'] = c['unique']
    return tables


def _col(t, name):
    for c in t['cols']:
        if c['name'] == name:
            return c
    raise KeyError(name)


if __name__ == '__main__':
    T = load()
    print(len(T), 'bảng;', sum(len(t['cols']) for t in T.values()), 'cột;', sum(len(t['fks']) for t in T.values()), 'khoá ngoại')
    for t in T.values():
        print(t['name'], [(c['name'], c['type'], 'N' if c['null'] else '') for c in t['cols']][:3], [f['name'] for f in t['fks']])
