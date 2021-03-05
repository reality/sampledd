## SampleDD

Sample patients who die during their stay from MIMIC-III, and their earliest associated text record.

### Usage

1. Download **NOTEEVENTS.csv** and **ADMISSIONS.csv** into a directory.
2. Run ``groovy sampledd.groovy <path to mimic directory> <train size> <valid size> <test size> <min offset> <event offset>``

This will produce sampled_patients.tsv with the sampled patients. Note: the train set is balanced 50/50, but the other sets are not. offset is used to determine the minimum amount of time the patient must have stayed alive / not been discharged to be counted, and is counted in integer hours from admission time. they must also have had at least one note event during the first [offset] hours. event offset is the max amount of integer hours after the min offset that we're looking at death occurring - this goes into the 'died before event cutoff column' as a boolean value, and is also used to balance the training set.

3. Run ``groovy extract_earliest_texts.groovy <path to mimic directory> <texts offset>`` to extract the earliest text record associated with each patient visit. They will be in texts/. offset is used to determine how many hours of the texts are stored (given as an integer). for example, if the value is 6, only texts from the first 6 hours of the patient's stay will be recordedj 
