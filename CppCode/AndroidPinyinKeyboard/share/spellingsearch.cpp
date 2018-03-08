#include "../include/spellingsearch.h"
namespace ime_pinyin {
	void SpellingSearch::traverse(SpellingNode* node)
	{
		if (node == 0) return;
		if (spl_trie_->is_full_id(node->spelling_idx)) {
			const char* ch = SpellingTrie::get_instance().get_spelling_str(node->spelling_idx);
			//printf("%s ", ch);
			int len = strlen(ch);
			ids_[len][id_nums_[len]] = node;
			id_nums_[len] ++;
		}		
		for (int i = 0; i < node->num_of_son; i++) {
			traverse(node->first_son + i);
		}
	}
	SpellingSearch::SpellingSearch()
	{

	}
	SpellingSearch::SpellingSearch(SpellingParser* spl_parser,const SpellingTrie* spl_trie)
	{
		spl_trie_ = spl_trie;
		for (int i = 0; i < kMaxPinyinSize+1; i++) {
			id_nums_[i] = 0;
		}
		//traverse the spelling trie
		//printf("\n\n======in traverse spelling trie======\n");

		SpellingNode* node = spl_trie->root_;
		for (int i = 0; i < 26; i++) {
			node = spl_trie->level1_sons_[i];
			if (node != 0) {
				traverse(node);
			}
		}		
		//printf("======end traverse spelling trie======\n\n\n");
	}
	SpellingSearch::~SpellingSearch()
	{
	}
	int SpellingSearch::searchFullSpellingIds(TouchModel* touchModel, uint16 * splIds, double * scores, 
		                                      Position * userPos, char * actualCharPos, int length)
	{
		if (kPrintDebug1) {
			for (int i = 0; i < length; i++) {
				printf("pos: %d: %f,\t%f\n", i, userPos[i].x, userPos[i].y);
			}
		}
		int ret_num = id_nums_[length];
		for (int i = 0; i < id_nums_[length]; i++) {
			SpellingNode* node = ids_[length][i];
			const char* ch = SpellingTrie::get_instance().get_spelling_str(node->spelling_idx);
			double probScore = 0.0;
			
			for (int j = 0; j < length; j++) {
				probScore += (touchModel->getLogProb(ch[j], userPos[j]))*static_cast<double>(-100);
				char actual = touchModel->getActualChar(userPos[j]);
				if (ch[j] >= 'A' && ch[j] <= 'Z') {
					actual = actual - 'a' + 'A';
				}
				//printf("%d:actual:%c, ch:%c\n", j, actual, ch[j]);
				if (ch[j] != actual) {
						probScore += (800);
				}
			}
			probScore /= length;
			//printf("calculating : %s\t%f\n", ch, probScore);
			//probScore *= (kMaxPinyinSize / length);
			int no = i;
			//uint16 intScore = static_cast<uint16>(probScore);
			while (no > 0 && (scores[no-1] > probScore)) {
				scores[no] = scores[no - 1];
				splIds[no] = splIds[no - 1];
				no--;
			} 
			//printf("ordered pos: %d\n", no);
			scores[no] = probScore;
			splIds[no] = node->spelling_idx;
			//printf("%s %d, %f\n", ch, node->score, probScore);
			/*printf("=====ordered=====\n");
			for (int j = 0; j <= i; j++) {
				const char* cch = SpellingTrie::get_instance().get_spelling_str(splIds[j]);
				printf("%d: %d, %s, %f\n", j, splIds[j], cch, scores[j]);
			}*/
			//system("pause");
		}
		if (kPrintDebug1) {
			printf("====pinyin str start ===\n");
		}
		if (ret_num > 3) ret_num = 3;
		for (int i = ret_num - 1; i >= 0; i--) {
			if (scores[i] > 2100) {
				ret_num --;
			}
		}
		if (ret_num < 0) ret_num = 0;
		if (kPrintDebug1) {
			for (int i = 0; i < 5; i++) {
				const char* ch = SpellingTrie::get_instance().get_spelling_str(splIds[i]);
				printf("%d: %d, %s, %f\n", i, splIds[i], ch, scores[i]);
			}
		}
		if (kPrintDebug1) {
			printf("====pinyin str end: %d===\n", ret_num);
		}
		//system("pause");
		return ret_num;
	}
}// namespace ime_pinyin
