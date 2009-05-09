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


def Int3ByteToStr(num):
  assert num >= 0
  assert num < 2**24
  result = ''
  for _ in range(3):
    char = chr(num & 255)
    result += char
    num = num >> 8
  return result


def GetMinLatLng(geo_objects):
  assert len(geo_objects) > 0
  min_lat, min_lng = geo_objects[0].latlng
  for geo_object in geo_objects:
    lat, lng = geo_object.latlng
    if lat < min_lat:
      min_lat = lat
    if lng < min_lng:
      min_lng = lng

  return (min_lat, min_lng)


def GetNormalizedTitle(geo_object):
  title = unicode(geo_object.title, 'utf8').lower()
  title = title.replace('\n', ' ')
  title = title.replace('\r', ' ')
  return title
  

def WriteCoords(geo_objects, out_file):
  out_file.write(IntToStr(len(geo_objects)))

  min_lat, min_lng = GetMinLatLng(geo_objects)
  min_lat_int = int(min_lat * 1e+7)
  min_lng_int = int(min_lng * 1e+7)
  out_file.write(IntToStr(min_lat_int) + IntToStr(min_lng_int))

  for geo_object in geo_objects:
    lat, lng = geo_object.latlng
    lat_int = int((lat - min_lat) * 1e+7)
    out_file.write(Int3ByteToStr(lat_int))

  for geo_object in geo_objects:
    lat, lng = geo_object.latlng
    lng_int = int((lng - min_lng) * 1e+7)
    out_file.write(Int3ByteToStr(lng_int))
    
  print 'Wrote %d coordinate entries' % len(geo_objects)


class TitleWriter(object):
  # Implementers should override tag with unique positive value.
  tag = 0

  def EncodeString(self, string):
    raise NotImplemented()

  def WriteHeader(self, out_file):
    raise NotImplemented()


class UTF16FormatWriter(TitleWriter):
  tag = 1

  def EncodeString(self, string):
    return title.encode('utf-16le')

  def WriteHeader(self, out_file):
    pass


class CustomCharsetFormatWriter(TitleWriter):
  tag = 2

  def __init__(self):
    self.char2byte = {}
    self.byte2char = {}

  def BuildCharset(self, geo_objects):
    chars = set('\n')
    for geo_object in geo_objects:
      title = GetNormalizedTitle(geo_object)
      for ch in title:
        chars.add(ch)

    print 'Total', len(chars), 'different chars'
    cnt = 0
    chars = list(chars)
    chars.sort()

    if len(chars) > 220:
      return False

    num_chr = 0
    for char in chars:
      self.char2byte[char] = num_chr
      self.byte2char[num_chr] = char
      num_chr += 1
    return True

  def EncodeString(self, string):
    assert type(string) == unicode
    result = ''
    for ch in string:
      code = self.char2byte[ch]
      result += chr(code)
    return result

  def WriteHeader(self, out_file):
    out_file.write(IntToStr(len(self.byte2char)))
    for code in range(len(self.byte2char)):
      out_file.write(IntToStr(ord(self.byte2char[code])))


def SelectOptimalWriter(geo_objects):
  writer = CustomCharsetFormatWriter()
  if writer.BuildCharset(geo_objects):
    return writer
  return UTF16FormatWriter()
      

def WriteTitles(geo_objects, out_file):
  writer = SelectOptimalWriter(geo_objects)
  assert writer
  print 'Using %s' % writer.__class__.__name__
  out_file.write(IntToStr(writer.tag))

  # Calculate total number of characters first.
  total_chars = 0
  for geo_object in geo_objects:
    title = GetNormalizedTitle(geo_object)
    title += '\n'
    total_chars += len(title)

  out_file.write(IntToStr(len(geo_objects)) + IntToStr(total_chars))

  writer.WriteHeader(out_file)

  # Write titles - this must be in sync with the previous loop.
  cnt = 0
  num_chars = 0
  file_pos = 0
  for geo_object in geo_objects:
    title = GetNormalizedTitle(geo_object)
    # print cnt, num_chars, file_pos, geo_object.obj_id, title.encode('utf8'), geo_object.latlng
    title += '\n'
    encoded_title = writer.EncodeString(title)
    out_file.write(encoded_title)
    num_chars += len(title)
    file_pos += len(encoded_title)
    cnt += 1
    
  assert num_chars == total_chars
  
  print 'Wrote %d titles' % len(geo_objects)
  

def MakeIndex():
  geo_objects = []
  if _OPTIONS.plain_text_files:
    plain_text_files = _OPTIONS.plain_text_files.split(',')
    for text_file in plain_text_files:
      geo_objects += ReadPlainTextObjects(text_file)
  if _OPTIONS.wikimapia_dir:
    geo_objects += ReadWikimapiaObjects(_OPTIONS.wikimapia_dir)
  title_file = open('string.dat', 'w')
  coord_file = open('index.dat', 'w')
  WriteCoords(geo_objects, coord_file)
  WriteTitles(geo_objects, title_file)
  title_file.close()
  coord_file.close()


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
