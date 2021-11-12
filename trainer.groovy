#!/usr/bin/env groovy

/* Blackjack trainer
   =================
   Options
   1. [Multiple (2, 4, 6, 8) | Single] deck(s).
   2. Dealer [stands | hits] on soft 17.
   3. Double on any 2 cards. Double [allowed | not allowed] after split.
   4. Surrender [allowed | not allowed]. */

def init_game() {
    num_decks = 2
    dealer_hits_soft17 = false
    double_allowed_after_split = false
    surrender_allowed = true
    running_total = 100.0
    total_attempts = 0
    accurate_attempts = 0
    clear_screen()
    println 'You have been awarded 100 free chips!!! Good luck!' + '\7'
    System.console().readLine '(press any key to continue)'
    return [running_total, num_decks, dealer_hits_soft17, double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts]
}

def clear_screen() {
    System.out.print("\033[H\033[2J")
    System.out.flush()
}

def init_shoe(num_decks) {
    shoe = []
    def suits = ['H','C','D','S']
    def ranks = ['2','3','4','5','6','7','8','9','0','J','Q','K','A']
    def vals = [2,3,4,5,6,7,8,9,10,10,10,10,11] * (4 * num_decks)
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
    System.out.print("\033[H\033[2J")
    System.out.flush()
    return shoe
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
        def input = System.console().readLine "Enter wager [${wager.toString()}]: "
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
    hands = [0:[]]
    def dealers_hand = []
    hands[0].add(shoe.remove(0))
    dealers_hand.add(shoe.remove(0))
    hands[0].add(shoe.remove(0))
    dealers_hand.add(shoe.remove(0))
    return [hands, dealers_hand]
}

def show_start(hands, dealers_hand, default_style) {
    clear_screen()
    printf "Dealer's Upcard: "
    style = colorize(dealers_hand[1][0])
    printf style + dealers_hand[1][0]
    printf default_style + ' '
    printf '\n'
    println()
    for (hand in hands) {
        (hand_score, hand_ranks) = get_hand_info(hand)
        if (hands.size() == 1) {
            printf 'Your Hand: '
        } else {
            printf 'Your Hand ' + (hand.key + 1).toString() + ': '
        }
        for (card in hand.value) {
            style = colorize(card[0])
            printf style+card[0]
            printf default_style+' '
        }
        printf '(' + hand_score + ')'
        printf '\n'
    }
}

def get_action(hands, wager, surrender_allowed, running_total, default_style) {
    busted = false
    surrendered = false
    blackjack = false
    options = ['Press <Enter> to stand or enter H to hit']
    valid_actions = ['', 'H']
    for (hand in hands) {
        if (hands.size() > 1) {
            printf '[Hand ' + (hand.key + 1).toString() + '] '
        }
        (hand_score, hand_ranks) = get_hand_info(hand)
        if (hand_score == 21) {
            if (hand.value.size() == 2) {
                blackjack = true
            }
            action = ''
        }
        else if (hand_score > 21) {
            busted = true
            action = ''
        } else {
            if (hand.value.size() == 2) {
                options.add('D to double')
                valid_actions.add('D')
            }
            if (hand.value.size() == 2 && hand.value[0][0][0] == hand.value[1][0][0]) {
                options.add('S to split')
                valid_actions.add('S')
            }
            if (surrender_allowed) {
                options.add('Q to surrender')
                valid_actions.add('Q')
            }
            def prompt = ''
            if (options.size() > 1) {
                for (i = 0; i < (options.size() - 1); i++) {
                    prompt += options[i] + ', '
                }
                prompt += 'or ' + options[options.size() - 1] + ': '
            } else {
                prompt = options[0] + ': '
            }
            def action_err = true
            while (action_err) {
                action = System.console().readLine prompt
                action = action.trim().toUpperCase()
                if (action in valid_actions) {
                    action_err = false
                    if (action == 'Q') {
                        surrendered = true
                    }
                } else {
                    println 'Invalid entry. Try again.'
                }
            }
        }
    }
    return [action, busted, surrendered, blackjack]
}

def check_strategy(hand, dealers_hand, num_decks, wager, action) {
    correct_strategy = false
    (hand_score, hand_ranks) = get_hand_info(hand)
    // A,8
    if (hand_ranks as Set == ['A','8'] as Set && action == ''.toString()) {
        correct_strategy = true
    }
    // A,7
    else if (hand_ranks as Set == ['A','7'] as Set) {
        if (dealers_hand[1][0][0] in ['2','7','8'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['3','4','5','6'] && action == 'D') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // A,6
    else if (hand_ranks as Set == ['A','6'] as Set) {
        if (dealers_hand[1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['3','4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,5
    else if (hand_ranks as Set == ['A','5'] as Set) {
        if (dealers_hand[1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,4
    else if (hand_ranks as Set == ['A','4'] as Set) {
        if (dealers_hand[1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,3
    else if (hand_ranks as Set == ['A','3'] as Set) {
        if (dealers_hand[1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,2
    else if (hand_ranks as Set == ['A','2'] as Set) {
        if (dealers_hand[1][0][0] in ['2', '3', '4', '7', '8', '9', '0', 'J', 'Q', 'K', 'A'] && action == 'H') {
            correct_strategy = true
        } else if (dealers_hand[1][0][0] in ['5', '6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // A,A
    else if (hand_ranks == ['A','A'] && action == 'S') {
        correct_strategy = true
    }
    // 10,10
    else if (hand_ranks == ['0','0'] && action == ''.toString()) {
        correct_strategy = true
    }
    // 9,9
    else if (hand_ranks == ['9','9']) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6','8','9'] && action == 'S') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['7','0','J','Q','K','A'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 8,8
    else if (hand_ranks == ['8','8'] && action == 'S') {
        correct_strategy = true
    }
    // 7,7
    else if (hand_ranks == ['7','7']) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6','7'] &&  action == 'S') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 6,6
    else if (hand_ranks == ['6','6']) {
        if (dealers_hand[1][0][0] in ['3','4','5','6'] && action == 'S') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 4,4
    else if (hand_ranks == ['4','4'] && action == 'H') {
        correct_strategy = true
    }
    // 3,3
    else if (hand_ranks == ['3','3']) {
        if (dealers_hand[1][0][0] in ['2','3','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['4','5','6','7'] && action == 'S') {
            correct_strategy = true
        }
    }
    // 2,2
    else if (hand_ranks == ['2','2']) {
        if (dealers_hand[1][0][0] in ['2','3','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['4','5','6','7'] && action == 'S') {
            correct_strategy = true
        }
    }
    // 17 or more
    else if (hand_score >= 17 && action == ''.toString()) {
        correct_strategy = true
    }
    // 16
    else if (hand_score == 16) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['7','8'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['9','0','J','Q','K','A'] && action == 'Q') {
            correct_strategy = true
        }
    }
    // 15
    else if (hand_score == 15) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['7','8','9','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['0','J','Q','K'] && action == 'Q') {
            correct_strategy = true
        }
    }
    // 13,14
    else if (hand_score in [13,14]) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 12
    else if (hand_score == 12) {
        if (dealers_hand[1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 11
    else if (hand_score == 11) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6','7','8','9','0','J','Q','K'] && action == 'D') {
            correct_strategy = true
            }
        else if (dealers_hand[1][0][0] == 'A' && action == 'H') {
            correct_strategy = true
        }
    }
    // 10
    else if (hand_score == 10) {
        if (dealers_hand[1][0][0] in ['2','3','4','5','6','7','8','9'] && action == 'D') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 9
    else if (hand_score == 9) {
        if (dealers_hand[1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (dealers_hand[1][0][0] in ['3','4','5','6'] && action == 'D') {
            correct_strategy = true
        }
    }
    // 8 or less
    else if (hand_score <= 8 && action == 'H') {
        correct_strategy = true
    }
    return correct_strategy
}

def take_action(hand, action) {
    if (action == 'H') {
        hand.value.add(shoe.remove(0))
    }
    else if (action == 'D') {
        wager += wager
        println "You have increased your wager to " + wager
        hand.value.add(shoe.remove(0))
    }
//    else if (action == 'S') {
//        wager += wager
//        println "You have increased your wager to " + wager
//        split_card=hands[hands.size()-1][1]
//        hands[hands.size()-1].remove(split_card)
//        hands[hands.size()]=split_card
//        hands[hands.size()-1].add(shoe.remove(0))
//        hands[hands.size()].add(shoe.remove(0))
//    }
    return hand
}

def get_hand_info(hand) {
    def hand_score = 0
    def hand_ranks = []
    for (card in hand.value) {
        hand_score += card[1].toInteger()
        hand_ranks.add(card[0][0])
    }
    for (rank in hand_ranks) {
        if (hand_score > 21 && rank == 'A') {
            hand_score -= 10
        }
    }
    return [hand_score, hand_ranks]
}

def results(dealers_hand,hand, running_total, blackjack, busted, surrendered) {
    (dealers_hand_score, dealers_hand_ranks) = get_hand_info(dealers_hand)
    (hand_score, hand_ranks) = get_hand_info(hand)
    if ((dealers_hand_score > 21 || dealers_hand_score < hand_score) && !busted && !surrendered) {
        println 'Winner!!!' + '\7'
        println()
        if (blackjack) {
            running_total += (wager * 1.5).round(2)
        } else running_total += wager
    }
    else if (dealers_hand_score > hand_score || busted) {
        println 'The house wins.'
        println()
        running_total -= wager
    }
    else if (surrendered) {
        println 'Your wager has been split with the house.'
        println()
        running_total -= (wager / 2).round(2)
    } else {
        println 'Push'
        println()
    }
    return running_total
}

def show_hand(hands, default_style) {
    for (hand in hands) {
        (hand_score, hand_ranks) = get_hand_info(hand)
        if (hands.size() == 1) {
            printf 'Your Hand: '
        } else {
            printf 'Your Hand ' + (hand.key + 1).toString() + ': '
        }
        for (card in hand.value) {
            style = colorize(card[0])
            printf style+card[0]
            printf default_style+' '
        }
        printf '(' + hand_score + ')'
        printf '\n'
    }
}

def show_dealers_hand(dealers_hand, default_style) {
    (dealers_hand_score, dealers_hand_ranks) = get_hand_info(dealers_hand)
    printf "Dealer's Hand: "
    for (card in dealers_hand) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '(' + dealers_hand_score + ')'
    printf '\n'
}

def dealer_hits(dealers_hand, default_style, busted, surrendered, blackjack) {
    (dealers_hand_score, dealers_hand_ranks) = get_hand_info(dealers_hand)
    show_dealers_hand(dealers_hand, default_style)
    if (!busted && !surrendered && !blackjack) {
        while (dealers_hand_score < 17) {
            sleep(1000)
            println 'Dealer hits.'
            dealers_hand.add(shoe.remove(0))
            show_dealers_hand(dealers_hand, default_style)
        }
    }
    if (dealers_hand_score > 21) {
        println 'Dealer busts!' + '\7'
    }
}

def react(hand) {
    correct_strategy = check_strategy(hand.value, dealers_hand, num_decks, wager, action)
    if (blackjack) {
        println 'Blackjack!' + '\7'
        println()
    } else {
        total_attempts += 1
        if (busted) {
            println 'You bust!' + '\7'
            println()
        }
        if (surrendered) {
            println 'You surrendered your hand.'
            println()
        }
        if (correct_strategy) {
            accurate_attempts += 1
            println 'Your strategy is correct.'
            println()
        } else {
            println 'Your strategy is incorrect.' + '\7'
            println()
        }
        if (action in ['H','D'] && !busted && !surrendered) {
            hand = take_action(hand, action)
        }
    }
}

def end_of_game(play_again, running_total) {
    play_again = false
    println 'Game Over.'
    if (running_total == 0) {
        println 'You leave with nothing. Play again if you dare!'
    }
    def again_input_err = true
    while (again_input_err) {
        def again_yn = System.console().readLine 'Press <Enter> to play again or enter Q to quit: '
        if (again_yn.trim() == '') {
            again_input_err = false
            play_again = true
            System.out.print("\033[H\033[2J")
            System.out.flush()
        }
        else if (again_yn.toUpperCase() == 'Q') {
            again_input_err = false
            if (running_total > 0) {
                println 'Credit balance. Cash dispensed below.'
            } else {
                 println 'Balance due. Insert credit card below.'
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
    def play_again = true
    def default_style = "${(char)27}[37;40m"
    (running_total, num_decks, dealer_hits_soft17, double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts) = init_game()
    shoe = init_shoe(num_decks)
    Collections.shuffle(shoe)

    while (play_again) {
        wager = init_wager(running_total, total_attempts, accurate_attempts)
        (hands, dealers_hand) = deal(shoe)
        show_start(hands, dealers_hand, default_style)
        (action, busted, surrendered, blackjack) = get_action(hands, wager, surrender_allowed, running_total, default_style)
        for (hand in hands) {
            react(hand)
        }
        while (action !in ['','Q']) {
            show_hand(hands, default_style)
            (action, busted, surrendered, blackjack) = get_action(hands, wager, surrender_allowed, running_total, default_style)
            for (hand in hands) {
                react(hand)
            }
        }
        if (action == 'S') {
            split_card = hands[0][1]
            hands[0].remove(split_card)
            hands[1] = split_card
            hands[0].add(shoe.remove(0))
            hands[1].add(shoe.remove(0))
        }
        show_hand(hands, default_style)
        dealer_hits(dealers_hand, default_style, busted, surrendered, blackjack)
        for (hand in hands) {
            running_total = results(dealers_hand, hand, running_total, blackjack, busted, surrendered)
        }
//        if (accurate_attempts != total_attempts) {
//            println 'Game Over.'
//            println 'Your accuracy has fallen below 100%.' + '\7'
//
        play_again = false
//            if (running_total > 0) {
//                println 'Credit balance. Cash dispensed below.'
//            } else {
//                 println 'Balance due. Insert credit card below.'
//            }
//        } else {
//
        play_again = end_of_game(play_again, running_total)
//        }
    }
}

mainMethod()
