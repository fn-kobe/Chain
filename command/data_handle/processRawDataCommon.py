import os
import re

def getAllFileUnderOneFolder(path,all_files=[]):
    if os.path.exists(path):
        files=os.listdir(path)
    else:
        print('this path not exist: ' + path + "\n")

    for file in files:
        if os.path.isdir(os.path.join(path,file)):
            getAllFileUnderOneFolder(os.path.join(path,file),all_files)
        else:
            all_files.append(os.path.join(path,file))
    return all_files

# 定义函数
def list_all_files(rootdir):
    import os
    _files = []
	# 列出文件夹下所有的目录与文件
    list = os.listdir(rootdir)
    for i in range(0, len(list)):
		# 构造路径
        path = os.path.join(rootdir, list[i])
		# 判断路径是否为文件目录或者文件
		# 如果是目录则继续递归
        if os.path.isdir(path):
            _files.extend(list_all_files(path))
        if os.path.isfile(path):
            _files.append(path)

    return _files

def getValueFromFile(fileName, key):
    result = -1
    with  open(fileName, 'r', encoding='utf-8') as oFile:
        lineContent = oFile.readline()
        while ('' != lineContent):
            ret = re.search(key, lineContent)
            if ret:
                result = ret.group(1)
                return result

            lineContent = oFile.readline()
    return result

def getOneTypeValues(folder, fileKey, valueKey):
    files = list_all_files(folder)

    filesSet = set(files)
    files = list(filesSet)
    return getOneTypeValuesFromFiles(files, fileKey, valueKey)

# used to reduce the access to get all files from one folder each time to get high efficiency
def getOneTypeValuesFromFiles(files, fileKey, valueKey):
    results = []
    for file in (files):
        ret = re.search(fileKey, file)
        if ret:
            value = getValueFromFile(file, valueKey)
            if (value != -1):
                v = float(value)
                if (v < 850): # exclude too large value
                    results.append(v)

    return results

def getAllValues(folder, file_key_array, search_key_array, name_key_array):
    files = list_all_files(folder)
    return getAllValuesFromFiles(files, file_key_array, search_key_array, name_key_array)

def getAllValuesFromFiles(files, file_key_array, search_key_array, name_key_array):
    results = dict()
    i = 0
    while (i < len(file_key_array)):
        file_key  = file_key_array[i]
        word_key = search_key_array[i]
        name_key = name_key_array[i]
        values = getOneTypeValuesFromFiles(files, file_key, word_key)
        results.update({name_key: values})
        i += 1

    return results