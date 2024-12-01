# Assignment4
Audio classification system (audio signal processing and ML, can use system integration with os ML)
Req: group speech files, group music files, can use 40 short audio files (20 speech, 20 music)
Operational Steps: develop a program to read each audio file and extract audio features, for short audio clips can treat each one as a single unit. (Can use NAudio)


NOTES: 
Many types of ML algorithms:
 supervised learning, (and classification (music?))
To prepare for ML process, can be formatted as: #, f1, f2, f3, label (filename, features, label = yes if file is music, no if speech)
Use about ⅔ of data for training data to build the music model 
After training, do testing/validation, since this is a testing dataset, we already have ground truth data (GT), the ML model should provide the answer. MODEL - GT
YES - YES (true positive, or tp)
YES - NO (false positive, or fp)
NO - YES (false negative, or fn)
NO - NO (true negative, or tn)
Takeaway: Compare the GT with MODEL to get tp, fp, fn, or tn values and use them ro compute precision and recall 


UNsupervised learning (and clustering)
Don’t provide training data, jump into data stream and see what you can do 
Clustering: segment data into separate groups 
Association Rule Mining: supermarket, put related items together (baby lotion and baby formula in same area) or online shopping has recommendations for similar products 


REPORT: 
1.how to run code, use system, functionalities of each program file 
list/brief intro to libraires/tools/techniques you used
Least features + values for audio files and show what files are used as training vs test data
Show comparison between model output and GT, display precision and recall value of your model on the testing data
