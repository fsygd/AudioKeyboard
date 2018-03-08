#include "../include/touchmodel.h"

namespace ime_pinyin {
	TouchModel::TouchModel() {
		srand(time(0));
		for (int i = 0; i < 26; i++) {
			keyCenters[i][0] = (keyBoundary[i][0] + keyBoundary[i][1]) / 2.0;
			keyCenters[i][1] = (keyBoundary[i][2] + keyBoundary[i][3]) / 2.0;
			//printf("%c: %f,%f\n",'a'+i, keyCenters[i][0], keyCenters[i][1]);
		}
	}
	double TouchModel::getGaussianRand(double mean, double std) {
		double ret, V1, V2, S;
		do {
			double U1 = (double)rand() / RAND_MAX;
			double U2 = (double)rand() / RAND_MAX;
			V1 = 2 * U1 - 1;
			V2 = 2 * U2 - 1;
			S = V1 * V1 + V2 * V2;
		} while (S >= 1 || S == 0);
		ret = V1 * sqrt(-2 * log(S) / S);
		return (ret * std + mean);
	}
	double TouchModel::getGaussianProb(double x, double mean, double std)
	{
		return (exp(-(x - mean)*(x - mean) / 2.0 / std / std)) / sqrt(2 * PI*std*std);
	}
	double TouchModel::getLogGaussianProb(double x, double mean, double std)
	{
		return (-(x - mean)*(x - mean) / 2.0 / std / std) - 0.5 * log(2.0 * PI * std * std);
	}
	bool TouchModel::inKey(char key, Position pos)
	{
		int keyNo = key - 'a';
		return ((keyBoundary[keyNo][0] <= pos.x) && (keyBoundary[keyNo][1] >= pos.x)
			&& (keyBoundary[keyNo][2] <= pos.y) && (keyBoundary[keyNo][3] >= pos.y));
	}
	double TouchModel::getDist(char key, Position pos)
	{
		double centerX = keyCenters[key - 'a'][0];
		double centerY = keyCenters[key - 'a'][1];
		return sqrt((pos.x - centerX)*(pos.x - centerX) + (pos.y - centerY)*(pos.y - centerY));
	}
	Position TouchModel::getRandomPos(char ch) {
		Position ret;
		ret.x = getGaussianRand(keyCenters[ch - 'a'][0], keyWidth*2.0 / 6.0);
		ret.y = getGaussianRand(keyCenters[ch - 'a'][1], keyHeight*2.0 / 6.0);
		//printf("get random pos:%c:%f, %f\n", ch, keyCenters[ch-'a'][0], ret.x);
		//printf("get random pos:%c:%f, %f\n", ch, keyCenters[ch - 'a'][1], ret.y);
		if (ret.x < 0) ret.x = 0;
		if (ret.y < 0) ret.y = 0;
		return ret;
	}
	char TouchModel::getActualChar(Position pos)
	{
		for (char i = 'a'; i <= 'z'; i++) {
			if (inKey(i, pos)) {
				return i;
			}
		}
		double minDist = INFINITY;
		char minChar = 'a';
		for (char i = 'a'; i <= 'z'; i++) {
			double dist = getDist(i, pos);
			if (dist < minDist) {
				minDist = dist;
				minChar = i;
			}
		}
		return minChar;
	}
	double TouchModel::getProb(char ch, Position pos)
	{
		int charNo = ch - 'a';
		if (ch >= 'A') charNo = ch - 'A';
		double ret = 1.0;
		ret *= getGaussianProb(pos.x, keyCenters[charNo][0], keyWidth * 2.0 / 6.0);
		ret *= getGaussianProb(pos.y, keyCenters[charNo][1], keyHeight * 2.0 / 6.0);		
		return ret;
	}
	double TouchModel::getLogProb(char ch, Position pos)
	{
		int charNo = ch - 'a';
		if (ch >= 'A' && ch<='Z') charNo = ch - 'A';
		double ret = 0.0;
		ret += getLogGaussianProb(pos.x, keyCenters[charNo][0], keyWidth * 2.0 / 6.0);
		ret += getLogGaussianProb(pos.y, keyCenters[charNo][1], keyHeight * 2.0 / 6.0);
		return ret;
	}
}