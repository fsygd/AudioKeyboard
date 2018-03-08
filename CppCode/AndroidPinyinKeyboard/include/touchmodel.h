#pragma once
#ifndef TOUCH_MODEL_H
#define TOUCH_MODEL_H
#include <cstdio>
#include <stdlib.h>
#include <math.h>
#include <time.h>
namespace ime_pinyin {
#define PI 3.1415926535
	typedef struct Position {
		double x;
		double y;
	} Position;
	class TouchModel {
	private:
		int keyBoundary[26][4] = {// left, right, top, bottom
			{58, 205, 217, 434}, //a
			{ 793,940,434,651 }, //b
			{ 499,646,434,651 }, //c
			{ 352,499,217,434 }, //d
			{ 288,432,0,217 }, //e
			{ 499,646,217,434 }, //f
			{ 646,793,217,434 }, //g
			{ 793,940,217,434 }, //h
			{ 1008,1152,0,217 }, //i
			{ 940,1087,217,434 }, //j
			{ 1087,1234,217,434 }, //k
			{ 1234,1381,217,434 }, //l
			{ 1087,1234,434,651 }, //m
			{ 940,1087,434,651 }, //n
			{ 1152,1296,0,217 }, //o
			{ 1296,1440,0,217 }, //p
			{ 0,144,0,217 }, //q
			{ 432,576,0,217 }, //r
			{ 205,352,217,434 }, //s
			{ 576,720,0,217 }, //t
			{ 864,1008,0,217 }, //u
			{ 646,793,434,651 }, //v
			{ 144,288,0,217 }, //w
			{ 352,499,434,651 }, //x
			{ 720,864,0,217 }, //y
			{ 205,352,434,651 } //z
		};
		double keyCenters[26][2];
		double keyWidth = 144;
		double keyHeight = 542 - 325;

		double getGaussianRand(double mean, double std);
		double getGaussianProb(double x, double mean, double std);
		double getLogGaussianProb(double x, double mean, double std);
		bool inKey(char key, Position pos);
		double getDist(char key, Position pos);
	public:
		TouchModel();
		Position getRandomPos(char ch);
		char getActualChar(Position pos);
		double getProb(char ch, Position pos);
		double getLogProb(char ch, Position pos);
	};
}
#endif
