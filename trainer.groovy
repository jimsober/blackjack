#!/usr/bin/env groovy

/* Blackjack trainer
   =================
   Options
   1. [Multiple (2, 4, 6, 8) | Single] deck(s).
   2. Dealer [stands | hits] on soft 17.
   3. Double on any 2 cards. Double [allowed | not allowed] after split.
   4. Surrender [allowed | not allowed]. */

def init_game() {
    default_style = "${(char)27}[37;40m"
    num_decks = 2
    dealer_hits_soft17 = false
    double_allowed_after_split = false
    surrender_allowed = true
    running_total = 100.0
    total_attempts = 0
    accurate_attempts = 0
    System.out.print("\033[H\033[2J")
    System.out.flush()    println 'You have been awarded 100 free chips!!! Good luck!' + '\7'
    System.console().readLine '(press any key to continue)'
    return [default_style, running_total, num_decks, dealer_hits_soft17, double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts]
}

def init_shoe(num_decks) {
    shoe = []
    suits = ['H','C','D','S']
    ranks = ['2','3','4','5','6','7','8','9','0','J','Q','K','A']
    vals = [2,3,4,5,6,7,8,9,10,10,10,10,11] * (4 * num_decks)
    for (i = 0; i < num_decks; i++) {
        for (suit in suits) {
            for (rank in ranks) {
                shoe.add([rank + suit,0])
            }
        for (j = 0; j < shoe.size(); j++) {
            shoe[j][1] = vals[j]
            }
        }
    }
    Collections.shuffle(shoe)
    num_cards = shoe.size()
    cut_index = Math.abs( new Random().nextInt() % (num_cards - (num_cards * .75).intValue()) ) + (num_cards * .75).intValue()
    shoe.addAll(cut_index,[['CC',0]])
    cut_card_drawn = false
    println 'The shoe has been shuffled.' + '\7'
    return [shoe, cut_card_drawn]
}

def init_wager(running_total, total_attempts, accurate_attempts) {
    if (total_attempts != 0) {
        println 'Accuracy: ' + (100 * (accurate_attempts / total_attempts)).round(1).toString() + '%'
    }
    cash = running_total - running_total.intValue()
    if (cash > 0) {
        println 'Balance: ' + running_total.intValue().toString() + ' chips and $' + cash.toString()
    } else {
        println 'Balance: ' + running_total.intValue().toString()
    }
    try {
        wager
    } catch (MissingPropertyException) {
        if (running_total >= 10) {
            wager = 5
        } else {
            wager = running_total
        }
    }
    if (wager > running_total.intValue() / 2) {
        wager = (running_total.intValue() / 2).intValue()
    }

    input_err = true
    while (input_err) {
        input = System.console().readLine "Enter wager [${wager.toString()}]: "
        if (input.trim() == '') {
            input_err = false
            input = wager
        } else {
            try {
                assert input.toInteger() > 0
                try {
                    assert input.toInteger() <= running_total - input.toInteger()
                    input_err = false
                    wager = input.toInteger()
                } catch (AssertionError ignored) {
                    println 'Wager must be ' + (running_total.intValue() / 2).intValue() + ' or less so it can be doubled or split.'+ '\7'
                }
            } catch (AssertionError ignored) {
                println 'Wager must be greater than zero.' + '\7'
            } catch (ValueError) {
                println 'Wager must be a number greater than zero.' + '\7'
            }
        }
    }
    return wager
}

def deal(shoe) {
    hands_played = 0
    hands = []
    hands[0] = [[],0,[],false]
    hands[1] = [[],0,[],false]
    deal_card(hands[1][0])
    deal_card(hands[0][0])
    deal_card(hands[1][0])
    deal_card(hands[0][0])
    return [hands, hands_played]
}

def deal_card(hand) {
    drawn_card = shoe.remove(0)
    if (drawn_card[0] == 'CC') {
        cut_card_drawn = true
        println 'The cut card has been drawn.' + '\7'
        hand.add(shoe.remove(0))
    } else {
        hand.add(drawn_card)
    }
    get_hand_info()
    return [hands, cut_card_drawn]
}

def show_start(hands, default_style) {
    printf "Dealer's Upcard: "
    style = colorize(hands[0][0][1][0])
    printf style + hands[0][0][1][0]
    printf default_style + ' '
    printf '\n'
    println()
    get_hand_info()
    printf 'Your Hand: '
    for (card in hands[1][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '(' + hands[1][1] + ')'
    printf '\n'
}

def get_action(hands, hand_index, surrender_allowed) {
    if (hands[hand_index][1] == 21) {
        action = 'A'
    } else {
        options = ['Press <Enter> to stand or enter H to hit']
        valid_actions = ['', 'H']
        options.add('D to double')
        valid_actions.add('D')
        if (hands[hand_index][0].size() == 2 && hands[hand_index][0][0][0][0] == hands[hand_index][0][1][0][0]) {
            options.add('S to split')
            valid_actions.add('S')
        }
        if (surrender_allowed) {
            options.add('Q to surrender')
            valid_actions.add('Q')
        }
        prompt = ''
        if (options.size() > 1) {
            for (i = 0; i < (options.size() - 1); i++) {
                prompt += options[i] + ', '
            }
            prompt += 'or ' + options[options.size() - 1] + ': '
        } else {
            prompt = options[0] + ': '
        }
        action_err = true
        while (action_err) {
            action = System.console().readLine prompt
            action = action.trim().toUpperCase()
            if (action in valid_actions) {
                action_err = false
            } else {
                println 'Invalid entry. Try again.'
            }
        }
    }
    return action
}

def check_strategy(hands, hand_index, num_decks, surrender_allowed, wager, action) {
    correct_strategy = false
    //get_hand_info()
    // A,8
    if (hands[hand_index][2] as Set == ['A','8'] as Set && action == ''.toString()) {
        correct_strategy = true
    }
    // A,7
    else if (hands[hand_index][2] as Set == ['A','7'] as Set) {
        if (hands[0][0][1][0][0] in ['2','7','8'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['3','4','5','6'] && action == 'D') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // A,6
    else if (hands[hand_index][2] as Set == ['A','6'] as Set) {
        if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['3','4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,5
    else if (hands[hand_index][2] as Set == ['A','5'] as Set) {
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,4
    else if (hands[hand_index][2] as Set == ['A','4'] as Set) {
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,3
    else if (hands[hand_index][2] as Set == ['A','3'] as Set) {
        if (hands[0][0][1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,2
    else if (hands[hand_index][2] as Set == ['A','2'] as Set) {
        if (hands[0][0][1][0][0] in ['2', '3', '4', '7', '8', '9', '0', 'J', 'Q', 'K', 'A'] && action == 'H') {
            correct_strategy = true
        } else if (hands[0][0][1][0][0] in ['5', '6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,A
    else if (hands[hand_index][2] == ['A','A'] && action == 'S') {
        correct_strategy = true
    }
    // 10,10
    else if (hands[hand_index][2] == ['0','0'] && action == ''.toString()) {
        correct_strategy = true
    }
    // 9,9
    else if (hands[hand_index][2] == ['9','9']) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','8','9'] && action == 'S') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','0','J','Q','K','A'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 8,8
    else if (hands[hand_index][2] == ['8','8'] && action == 'S') {
        correct_strategy = true
    }
    // 7,7
    else if (hands[hand_index][2] == ['7','7']) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7'] &&  action == 'S') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 6,6
    else if (hands[hand_index][2] == ['6','6']) {
        if (hands[0][0][1][0][0] in ['3','4','5','6'] && action == 'S') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 4,4
    else if (hands[hand_index][2] == ['4','4'] && action == 'H') {
        correct_strategy = true
    }
    // 3,3
    else if (hands[hand_index][2] == ['3','3']) {
        if (hands[0][0][1][0][0] in ['2','3','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6','7'] && action == 'S') {
            correct_strategy = true
        }
    }
    // 2,2
    else if (hands[hand_index][2] == ['2','2']) {
        if (hands[0][0][1][0][0] in ['2','3','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6','7'] && action == 'S') {
            correct_strategy = true
        }
    }
    // 17 or more
    else if (hands[hand_index][1] >= 17) {
        if (hands[hand_index][3] && action == 'H') {
            correct_strategy = true
        }
        else if (!hands[hand_index][3] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 16
    else if (hands[hand_index][1] == 16) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','8'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['9','0','J','Q','K','A'] && ((surrender_allowed && action == 'Q') || (!surrender_allowed && action == 'H'))) {
            correct_strategy = true
        }
    }
    // 15
    else if (hands[hand_index][1] == 15) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','8','9','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K'] && ((surrender_allowed && action == 'Q') || (!surrender_allowed && action == 'H'))) {
            correct_strategy = true
        }
    }
    // 13,14
    else if (hands[hand_index][1] in [13,14]) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 12
    else if (hands[hand_index][1] == 12) {
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 11
    else if (hands[hand_index][1] == 11) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9','0','J','Q','K'] && action == 'D') {
            correct_strategy = true
            }
        else if (hands[0][0][1][0][0] == 'A' && action == 'H') {
            correct_strategy = true
        }
    }
    // 10
    else if (hands[hand_index][1] == 10) {
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9'] && action == 'D') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 9
    else if (hands[hand_index][1] == 9) {
        if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['3','4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // 8 or less
    else if (hands[hand_index][1] <= 8 && action == 'H') {
        correct_strategy = true
    }
    return correct_strategy
}

def take_action(hands, action, cut_card_drawn) {
    if (action == 'H') {
        (hands, cut_card_drawn) = deal_card(hands[hand_index][0])
    }
    else if (action == 'D') {
        println "You have increased your wager by " + wager
        println()
        (hands, cut_card_drawn) = deal_card(hands[hand_index][0])
    }
    else if (action == 'S') {
        println "You have increased your wager by " + wager
        println()
        split_card=  hands[hand_index][0][1]
        hands[hand_index][0].remove(split_card)
        hands[hand_index + 1] = [[],0,[],false]
        hands[hand_index + 1][0].add(split_card)
        (hands, cut_card_drawn) = deal_card(hands[hand_index][0])
        (hands, cut_card_drawn) = deal_card(hands[hand_index + 1][0])
    }
    get_hand_info()
    return [hands, cut_card_drawn]
}

def get_hand_info() {
    i = 0
    for (hand in hands) {
        hand_score = 0
        hand_ranks = []
        soft_ace = false
        for (card in hand[0]) {
            hand_score += card[1]
            hand_ranks.add(card[0][0])
        }
        for (rank in hand_ranks) {
            if (hand_score > 21 && rank == 'A') {
                hand_score -= 10
                soft_ace = true
            }
        }
        hands[i][1] = hand_score
        hands[i][2] = hand_ranks
        hands[i][3] = soft_ace
        i += 1
    }
}

def results(hands, running_total, blackjack, busted, surrendered) {
    if ((hands[0][1] > 21 || hands[0][1] < hands[hand_index][1]) && !busted && !surrendered) {
        println 'Winner!!!' + '\7'
        println()
        if (blackjack) {
            running_total += (wager * 1.5).round(2)
        } else running_total += wager
    }
    else if (surrendered) {
        println 'Your wager has been split with the house.'
        println()
        running_total -= (wager / 2).round(2)
    }
    else if (hands[0][1] > hands[hand_index][1] || busted) {
        println 'The house wins.'
        println()
        running_total -= wager
    }
    else if (hands[0][1] == hands[hand_index][1]) {
        println 'Push'
        println()
    } else {
        println 'Jackpot!'
    }
    return running_total
}

def show_hand(hands, default_style) {
    //get_hand_info()
    printf 'Your Hand: '
    for (card in hands[hand_index][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '(' + hands[hand_index][1] + ')'
    printf '\n'
}

def show_dealers_hand(hands, default_style) {
    //get_hand_info()
    printf "Dealer's Hand: "
    for (card in hands[0][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '(' + hands[0][1] + ')'
    printf '\n'
}

def dealer_hits(hands, default_style, dealer_hits_soft17,busted, surrendered, blackjack, cut_card_drawn) {
    //get_hand_info()
    show_dealers_hand(hands, default_style)
    if (!busted && !surrendered && !blackjack) {
        while (hands[0][1] < 17 || (dealer_hits_soft17 && hands[0][1] == 17 && hands[0][3])) {
            sleep(1000)
            println 'Dealer hits.'
            (hands, cut_card_drawn) = deal_card(hands[0][0])
            get_hand_info()
            show_dealers_hand(hands, default_style)
        }
    }
    if (hands[0][1] > 21) {
        println 'Dealer busts!' + '\7'
    }
    return cut_card_drawn
}

def reaction(hands, cut_card_drawn, action) {
    blackjack = false
    surrendered = false
    busted = false
    split = false
    if (action == 'A') {
        if (hands[hand_index][0].size() == 2) {
            blackjack = true
            println 'Blackjack!' + '\7'
        }
        println()
    } else {
        correct_strategy = check_strategy(hands, hand_index, num_decks, surrender_allowed, wager, action)
        total_attempts += 1
        if (correct_strategy) {
            accurate_attempts += 1
            println 'Your strategy is correct.'
        } else {
            println 'Your strategy is incorrect.' + '\7'
        }
        println()
        if (action == 'Q') {
            surrendered = true
            println 'You have surrendered your hand.'
            println()
        } else {
            if (action == 'S') {
                split = true
                (hands, cut_card_drawn) = take_action(hands, action, cut_card_drawn)
            } else if (action in ['H', 'D', 'S']) {
                (hands, cut_card_drawn) = take_action(hands, action, cut_card_drawn)
            }
            if (hands[hand_index][1] > 21) {
                busted = true
                println 'You bust!' + '\7'
                println()
            }
        }
    }
    show_hand(hands, default_style)
    return [hands, cut_card_drawn, blackjack, surrendered, busted, split]
}

def end_of_game(play_again, running_total) {
    play_again = false
    println 'Game Over.'
    if (running_total == 0) {
        println 'You leave with nothing. Play again if you dare!'
    }
    again_input_err = true
    while (again_input_err) {
        again_yn = System.console().readLine 'Press <Enter> to play again or enter Q to quit: '
        if (again_yn.trim() == '') {
            again_input_err = false
            play_again = true
            System.out.print("\033[H\033[2J")
            System.out.flush()
        }
        else if (again_yn.toUpperCase() == 'Q') {
            again_input_err = false
            if (running_total > 0) {
                println 'Credit balance of $' + String.format("%.2f", running_total) + '. Cash dispensed below.'
            } else {
                 println 'Balance due $' + String.format("%.2f", running_total) + '. Insert credit card below.'
            }
        } else {
            println 'Try again. ' + '\7'
        }
    }
    return play_again
}

def colorize(card) {
    if (card[1] == 'C') { style = "${(char)27}[34;40"+'m' }
    else if (card[1] == 'D') { style = "${(char)27}[91;40"+'m' }
    else if (card[1] == 'H') { style = "${(char)27}[31;40"+'m' }
    else if (card[1] == 'S') { style = "${(char)27}[94;40"+'m' }
    return style
}

def mainMethod() {
    (default_style, running_total, num_decks, dealer_hits_soft17, double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts) = init_game()
    (shoe, cut_card_drawn) = init_shoe(num_decks)
    play_again = true

    while (play_again) {
        hand_index = 1
        if (cut_card_drawn) {
            (shoe, cut_card_drawn) = init_shoe(num_decks)
        }
        wager = init_wager(running_total, total_attempts, accurate_attempts)
        (hands, hands_played) = deal(shoe)
        show_start(hands, default_style)
        //
        while (hands_played < hand_index) {
            action = get_action(hands, hand_index, surrender_allowed)
            (hands, cut_card_drawn, blackjack, surrendered, busted, split) = reaction(hands, cut_card_drawn, action)
            while (action !in ['','A','D','Q'] && !busted && !surrendered) {
                action = get_action(hands, hand_index, surrender_allowed)
                (hands, cut_card_drawn, blackjack, surrendered, busted, split) = reaction(hands, cut_card_drawn, action)
            }
            hands_played += 1
            cut_card_drawn = dealer_hits(hands, default_style, dealer_hits_soft17, busted, surrendered, blackjack, cut_card_drawn)
            running_total = results(hands, running_total, blackjack, busted, surrendered)
            if (split) {
                hand_index += 1
                split = false
            }
        }
        //        if (accurate_attempts != total_attempts) {
        //            println 'Game Over.'
        //            println 'Your accuracy has fallen below 100%.' + '\7'
        play_again = end_of_game(play_again, running_total)
        //        }
    }
}

mainMethod()
