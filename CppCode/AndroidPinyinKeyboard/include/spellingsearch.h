#pragma once
#ifndef _SPL_SEARCH_H_
#define _SPL_SEARCH_H_
#include "dictdef.h"
#include "touchmodel.h"
#include "spellingtrie.h"
#include "splparser.h"
#include "ngram.h"
#include <cstring>
namespace ime_pinyin {
	class SpellingSearch {
	private:
		
		const SpellingTrie* spl_trie_;
		void traverse(SpellingNode* node);
		SpellingNode* ids_[kMaxPinyinSize+1][400];
		int id_nums_[kMaxPinyinSize+1];
	public:
		SpellingSearch();
		SpellingSearch(SpellingParser* spl_parser, const SpellingTrie* spl_trie);
		~SpellingSearch();
		int searchFullSpellingIds(TouchModel* touchModel, uint16* splIds, double* scores, Position* userPos, char* actualCharPos, int length);
	};
}
#endif // !_SPL_SEARCH_H_

