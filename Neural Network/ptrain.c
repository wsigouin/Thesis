
#include <stdio.h>

#include "fann.h"

/*
* Main function to train and test email sets
* Training and verification happen first, and then testing happens automatically
* Emails and FANN library files not inlcuded for brevity
*/
int main()
{
	char outputFilename[] = "resultsSize/batch_1.txt";
	char *mode = "a";

	FILE *ofp;
	ofp = fopen(outputFilename, mode);



	fann_type *calc_out;
	//loop used to change size of hidden layer
	for (int l1 = 0; l1 <= 15; l1 += 1) {

			const unsigned int numInputs = 15;
			const unsigned int numOutputs = 1;
			const unsigned int numLayers = 3;
			const unsigned int layers[3] = { numInputs,l1, numOutputs };
			const float errorThreshold = (const float)0;
			const unsigned int maxEpochs = 1;
			const unsigned int reportFrequency = 100;
			const unsigned int minIterations = 1;
			const unsigned int maxIterations = 1000;
			float currMSE = 1.0;
			float minMSE = 1.0;

			unsigned int i = 0;
			struct fann *ann;
			struct fann *bestAnn;
			struct fann_train_data *data;

			unsigned int decimal_point;

			printf("Creating network.\n");
			ann = fann_create_standard_array(numLayers, layers);


			fann_set_activation_steepness_hidden(ann, 1);
			fann_set_activation_steepness_output(ann, 1);

			//activation functions
			fann_set_activation_function_hidden(ann, FANN_SIGMOID_SYMMETRIC);
			fann_set_activation_function_output(ann, FANN_SIGMOID);

			fann_set_train_stop_function(ann, FANN_STOPFUNC_BIT);
			fann_set_bit_fail_limit(ann, 0.01f);

			//BACKPROP algorithm
			fann_set_training_algorithm(ann, FANN_TRAIN_INCREMENTAL);
			fann_set_learning_rate(ann, 0.1);

			data = fann_read_train_from_file("out20/4000_50_1_train.data");
			fann_init_weights(ann, data);

			for (i = 0; i < maxIterations; i++) {

				//train
				data = fann_read_train_from_file("out20/4000_50_1_train.data");
				printf("Training network.\n");
				fann_train_on_data(ann, data, maxEpochs, reportFrequency, errorThreshold);

				fann_reset_MSE(ann);
				data = fann_read_train_from_file("out20/4000_50_1_validate.data");

				printf("Testing network. %f\n", fann_test_data(ann, data));
				
				//verification
				for (int j = 0; j < fann_length_train_data(data); j++)
				{
					calc_out = fann_test(ann, data->input[j], data->output[j]);
					printf("Phishing test -> %f, should be %f, difference=%f\n",
						calc_out[0], data->output[j][0],
						fann_abs(calc_out[0] - data->output[j][0]));
				}
				printf("%d MSE: %f\n", l1,fann_get_MSE(ann));
				if (fann_get_MSE(ann) >= minMSE) {
					if (i > minIterations) {
						printf("Done after %d iterations with MSE of %f %f", i, fann_get_MSE(ann), minMSE);
						break;
					}
				}
				else {
					minMSE = fann_get_MSE(ann);
				}

			}


			//test
			fann_print_connections(ann);
			fann_print_parameters(ann);
			printf("Testing network.\n");
			data = fann_read_train_from_file("out20/4000_50_1_test.data");

			double falsePositive = 0;
			double falseNegative = 0;

			double numPhishing = 0;
			double numLegit = 0;
			double numIncorrect = 0;
			int l = 0;


			for (l = 0; l < fann_length_train_data(data); l++)
			{
				calc_out = fann_test(ann, data->input[l], data->output[l]);
				float result = calc_out[0];
				float original = data->output[l][0];
				float error = (float)fann_abs(calc_out[0] - data->output[l][0]);

				printf("Result %f original %f error %f\n", result, original, error);

				//phishing
				if (original == 1) {
					numPhishing++;
					if (result < 0.5) {
						falseNegative++;
						numIncorrect++;
					}
				}

				//ham
				if (original == 0) {
					numLegit++;
					if (result >= 0.5) {
						falsePositive++;
						numIncorrect++;
					}
				}

			}

			//results reporting
			fprintf(ofp, "Test %d: FP: %f FN: %f AC: %f MSE: %f\n", l1, ((falsePositive / numLegit * 100)), ((falseNegative / numPhishing * 100)), (100 - (numIncorrect / (l + 1) * 100)), fann_get_MSE(ann));


			printf("False Postives %f\n", ((falsePositive / numLegit * 100)));
			printf("False Negatives %f\n", ((falseNegative / numPhishing * 100)));
			printf("Overall Accuracy %f\n", (100 - (numIncorrect / (l + 1) * 100)));

			printf("Num Phishing %f", (numPhishing));
			printf("\nCleaning up.\n");


			fann_save(ann, "phishing.net");

			decimal_point = fann_save_to_fixed(ann, "phising_fixed.net");
			fann_save_train_to_fixed(data, "phishing_fixed.data", decimal_point);

			fann_destroy_train(data);
			fann_destroy(ann);
	}
	fclose(ofp);

	return 0;
}
