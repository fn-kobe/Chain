import os
import re

import numpy as np
import matplotlib.pyplot as plt
from processRawDataCommon import  getAllFileUnderOneFolder, getAllValuesFromFiles, list_all_files

#name key - used to identify different test case
# such as in the legend of a figure
embed_name_2u_key  = "embed2"
embed_name_4u_key  = "embed4"
embed_name_8u_key  = "embed8"
embed_name_16u_key = "embed16"
embed_name_32u_key = "embed32"

separate_name_2u_key  = "separate2"
separate_name_4u_key  = "separate4"
separate_name_8u_key  = "separate8"
separate_name_16u_key = "separate16"
separate_name_32u_key = "separate32"

embed_name_key = [embed_name_2u_key, embed_name_4u_key, embed_name_8u_key, embed_name_16u_key, embed_name_32u_key]
separate_name_key = [separate_name_2u_key, separate_name_4u_key, separate_name_8u_key, separate_name_16u_key, separate_name_32u_key]

#file key - used to find correspoding files, such as for exchange with 2 user the file name is embed2 or separate2.
# Then in testSequenceRemoteSeparateSteps.pl the log file should in thi format
embed_File_2u_key  = "embed2_[\d]+.log"
embed_File_4u_key  = "embed4_[\d]+.log"
embed_File_8u_key  = "embed8_[\d]+.log"
embed_File_16u_key = "embed16_[\d]+.log"
embed_File_32u_key = "embed32_[\d]+.log"

separate_File_2u_key  = "separate2_[\d]+.log"
separate_File_4u_key  = "separate4_[\d]+.log"
separate_File_8u_key  = "separate8_[\d]+.log"
separate_File_16u_key = "separate16_[\d]+.log"
separate_File_32u_key = "separate32_[\d]+.log"

embed_File_key = [embed_File_2u_key, embed_File_4u_key, embed_File_8u_key, embed_File_16u_key, embed_File_32u_key]
separate_File_key = [separate_File_2u_key, separate_File_4u_key, separate_File_8u_key, separate_File_16u_key, separate_File_32u_key]

# content key - used to find key information in one test file
# for example, "Total time is ([\d]+)"  is for the time of the test
sameContentKey = "Total time is ([\d]+)"
embed_runTime_2u_key = sameContentKey
embed_runTime_4u_key = sameContentKey
embed_runTime_8u_key = sameContentKey
embed_runTime_16u_key = sameContentKey
embed_runTime_32u_key = sameContentKey

separate_runTime_2u_key = sameContentKey
separate_runTime_4u_key = sameContentKey
separate_runTime_8u_key = sameContentKey
separate_runTime_16u_key = sameContentKey
separate_runTime_32u_key = sameContentKey

embed_search_key = [embed_runTime_2u_key, embed_runTime_4u_key, embed_runTime_8u_key, embed_runTime_16u_key, embed_runTime_32u_key]
separate_search_key = [separate_runTime_2u_key, separate_runTime_4u_key, separate_runTime_8u_key, separate_runTime_16u_key, separate_runTime_32u_key]


def getValuesMap(folder):
    files = getAllFileUnderOneFolder(folder)

    resultsEmbed = getAllValuesFromFiles(files, embed_File_key, embed_search_key, embed_name_key)
    resultsSeparate = getAllValuesFromFiles(files, separate_File_key, separate_search_key, separate_name_key)
    results = resultsEmbed
    results.update(resultsSeparate)

    return results

def getDifferentValuesArray(folder):
    files = getAllFileUnderOneFolder(folder)

    resultsEmbed = getAllValuesFromFiles(files, embed_File_key, embed_search_key, embed_name_key)
    resultsSeparate = getAllValuesFromFiles(files, separate_File_key, separate_search_key, separate_name_key)

    resultsMapForEmbed = getAverageMapFromValueMap(resultsEmbed)
    resultsMapForSeparate = getAverageMapFromValueMap(resultsSeparate)

    embedList = list(resultsMapForEmbed.values())
    separateList = list(resultsMapForSeparate.values())

    results = np.array(separateList) - np.array(embedList)

    print(embedList)
    print(separateList)
    print(results)

    return results

def getAverageMapFromValueMap(dictValue):
    avgDict = dict();
    for key in (dictValue):
        meanV = np.mean(dictValue[key])
        avgDict.update({key: meanV})
    return avgDict

def drawBarFromMap(map):
    drawBarByKeyValue(map.keys(), map.values())

def drawBarByKeyValue(keys, values):
    plt.bar(keys, values, width=0.4)
    plt.xlabel('number of users')
    plt.ylabel('difference of exchange time/seconds')
    plt.show()

def processFromRawData():
    dir = "D:\\research\\topics\protocol\\transaction_association_and_division\\to\\new_6_20\\experiment\\embed_sc_new\\data"
    diffValue = getDifferentValuesArray(dir)
    diffMap = dict()

    i = 0
    for v in (diffValue):
        diffMap.update({diffName[i]: v})
        i += 1

    drawBarFromMap(diffMap)


def drawBarFromAfterAnalysis():
    keys = diffName
    values = [23.3, 27.9, 25.4, 23.2, 27.1]
    drawBarByKeyValue(keys, values)



diffName = ['2', '4', '8', '16', '32']
# we should use the diagram from the raw data, while there are some extremely large values due to various reason
# then we exclude the extreme data which is far from others, and use the drawBarFromAfterAnalysis
# processFromRawData()

drawBarFromAfterAnalysis()

# embedMethodValues = []
# separateMethodValues = []
# for userKey in embed_runTime_user_key:
#     embedMethodValues.append(getAvgValue(userKey))
#
# for userKey in separate_runTime_user_key:
#     separateMethodValues.append(getAvgValue(userKey))
#
# print("embed method values: ", embedMethodValues)
# print("separae method values: ", separateMethodValues)
#
# barWidth = 0.3
# xlable = np.arange(0, len(embedMethodValues))
# plt.bar(xlable - barWidth / 2, embedMethodValues, width=barWidth)
# plt.bar(xlable + barWidth / 2, separateMethodValues, width=barWidth)
#
#
# plt.xlabel('number of users')
# plt.ylabel('average exchange fee/lu')
# plt.legend(['embed method', 'separate method'], loc='upper right')
# plt.xticks(xlable, ["2", "4", "8", "16", "32"])
#
# plt.show()