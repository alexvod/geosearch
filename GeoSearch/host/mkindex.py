#!/usr/bin/python

import getopt
import os
import sys

_KNOWN_OPTIONS = ['wikimapia_dir=', 'bbox=',
                  'plain_text_files=',
                  ]


class GeoObject(object):
  """Single object for geocoding."""

  def __init__(self):
    self.title = None
    self.obj_id = None
    self.latlng = None
    self.source = None

  def __str__(self):
    return '%s-%s@%s,%s' % (self.obj_id, self.title, self.latlng[0], self.latlng[1])

  def __repr__(self):
    return str(self)


class Options(object):
  def __init__(self):
    self.wikimapia_dir = None
    self.min_lat = -1000
    self.max_lat = +1000
    self.min_lng = -1000
    self.max_lng = +1000


_OPTIONS = Options()


def ParseNames(name_str):
  names = name_str.split('\x1f')

  if len(names) > 1:
    def ParseOneName(s):
      lang = ord(s[0]) - 32
      title = s[1:]
      return (lang, title)
    return map(ParseOneName, names)

  names = []
  cur_name = ''
  cur_lang = 0
  for ch in name_str:
    if (ord(ch) < 34) and (ch != ' '):
      names.append((cur_lang, cur_name))
      cur_name = ''
      cur_lang = ord(ch) - 32
    else:
      cur_name += ch
  names.append((cur_lang, cur_name))
  return names


def GetTitle(name_str):
  # ru, en, fr
  _TRY_LANGS = [1, 0, 2]
  parsed_names = ParseNames(name_str)
  names = dict(parsed_names)
  for lang in _TRY_LANGS:
    if lang in names:
      return names[lang]
  # nothing found - return the first
  return parsed_names[0][1]


def ParseWikimapiaData(data):
  result = []
  lines = data.split('\n')
  count = 0
  header = lines[0].split('|')
  base_lng = int(header[3])
  base_lat = int(header[5])
  lines = lines[4:]
  for l in lines:
    if not l.strip():
      continue
    fields = l.split('|')

    try:
      obj_id = int(fields[0])
    except ValueError:
      continue
    min_lng = int(fields[1]) + base_lng
    max_lat = int(fields[2]) + base_lat
    size_lng = int(fields[3])
    size_lat = int(fields[4])
    center_lat = max_lat - size_lat / 2.0
    center_lng = min_lng + size_lng / 2.0

    geo_object = GeoObject()
    geo_object.obj_id = obj_id
    geo_object.title = GetTitle(fields[6])
    geo_object.latlng = (center_lat * 1e-7, center_lng * 1e-7)

    if InsideBBox(geo_object):
      result.append(geo_object)

    count += 1

  return result


def ReadFile(filename):
  in_file = open(filename, 'r')
  content = in_file.read()
  in_file.close()
  return content


def ReadWikimapiaObjects(wikimapia_root):
  all_objects = {}
  for rel_name in os.listdir(wikimapia_root):
    filename = wikimapia_root + '/' + rel_name
    wikimapia_data = ReadFile(filename)
    geo_objects = ParseWikimapiaData(wikimapia_data)
    for geo_object in geo_objects:
      geo_object.source = rel_name
      all_objects[geo_object.obj_id] = geo_object
  return all_objects.values()


def IntToStr(num):
  result = ''
  for _ in range(4):
    char = chr(num & 255)
    result += char
    num = num >> 8
  return result


def EntryToStr(lat, lng):
  lat_int = int(lat * 1e+7)
  lng_int = int(lng * 1e+7)
  return IntToStr(lat_int) + IntToStr(lng_int)


def WriteIndex(geo_objects, out_file, idx_file):
  idx_file.write(IntToStr(len(geo_objects)))
  cnt = 0
  for geo_object in geo_objects:
    title = unicode(geo_object.title.lower(), 'utf8').lower().encode('utf8')
    title = title.replace('\n', ' ')
    latlng = geo_object.latlng
    title += '\n'
    out_file.write(title)
    idx_file.write(EntryToStr(latlng[0], latlng[1]))
    #print cnt, title
    cnt += 1
  idx_file.write(EntryToStr(0, 0))
  print 'Wrote %d entries' % cnt


def MakeIndex():
  geo_objects = []
  if _OPTIONS.plain_text_files:
    plain_text_files = _OPTIONS.plain_text_files.split(',')
    for text_file in plain_text_files:
      geo_objects += ReadPlainTextObjects(text_file)
  if _OPTIONS.wikimapia_dir:
    geo_objects += ReadWikimapiaObjects(_OPTIONS.wikimapia_dir)
  out_file = open('string.dat', 'w')
  idx_file = open('index.dat', 'w')
  WriteIndex(geo_objects, out_file, idx_file)
  out_file.close()
  idx_file.close()


def ReadPlainTextObjects(filename):
  in_file = open(filename, 'r')
  result = []
  for line in in_file:
    sep = line.rfind('@')
    assert sep >= 0
    title = line[0:sep]
    coords = line[sep+1:].split(',')
    assert len(coords) == 2
    latlng = map(float, coords)
    
    geo_object = GeoObject()
    geo_object.obj_id = None
    geo_object.title = title
    geo_object.latlng = (latlng[0], latlng[1])

    result.append(geo_object)

  return result


def ParseBBox(bbox_str):
  coords = bbox_str.split(",")
  assert len(coords) == 4
  min_lat, min_lng, max_lat, max_lng = map(float, coords)
  _OPTIONS.min_lat = min_lat
  _OPTIONS.min_lng = min_lng
  _OPTIONS.max_lat = max_lat
  _OPTIONS.max_lng = max_lng


def InsideBBox(geo_object):
  lat, lng = geo_object.latlng
  if lat < _OPTIONS.min_lat or lat > _OPTIONS.max_lat:
    return False
  if lng < _OPTIONS.min_lng or lng > _OPTIONS.max_lng:
    return False
  return True


def main():
  opt_list, _ = getopt.gnu_getopt(sys.argv, '', _KNOWN_OPTIONS)
  opt_dict = dict(opt_list)
  _OPTIONS.wikimapia_dir = opt_dict['--wikimapia_dir']
  _OPTIONS.plain_text_files = opt_dict.get('--plain_text_files', '')
  bbox = opt_dict.get('--bbox', '')
  if bbox:
    ParseBBox(bbox)

  MakeIndex()


if __name__ == '__main__':
  main()
