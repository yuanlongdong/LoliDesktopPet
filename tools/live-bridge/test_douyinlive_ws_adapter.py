#!/usr/bin/env python3
import unittest

from douyinlive_ws_adapter import to_pet_event


class DouyinLiveAdapterTests(unittest.TestCase):
    def test_chat_to_comment(self):
        self.assertEqual(
            to_pet_event({
                "method": "WebcastChatMessage",
                "user": {"nickname": "观众甲"},
                "content": "桌宠你好",
            }),
            {"type": "comment", "user": "观众甲", "text": "桌宠你好"},
        )

    def test_gift_to_gift_with_amount(self):
        self.assertEqual(
            to_pet_event({
                "method": "WebcastGiftMessage",
                "user": {"nickname": "观众乙"},
                "gift": {"name": "小心心", "diamondCount": 2},
                "repeatCount": 3,
            }),
            {"type": "gift", "user": "观众乙", "item": "小心心", "count": 3, "amount": 6},
        )

    def test_like_to_like(self):
        self.assertEqual(
            to_pet_event({
                "method": "WebcastLikeMessage",
                "user": {"nickname": "观众丙"},
                "count": 12,
            }),
            {"type": "like", "user": "观众丙", "count": 12},
        )

    def test_member_to_enter(self):
        self.assertEqual(
            to_pet_event({
                "method": "WebcastMemberMessage",
                "user": {"nickname": "观众丁"},
            }),
            {"type": "enter", "user": "观众丁"},
        )

    def test_follow_to_follow(self):
        self.assertEqual(
            to_pet_event({
                "method": "WebcastSocialMessage",
                "user": {"nickname": "观众戊"},
                "action": 1,
            }),
            {"type": "follow", "user": "观众戊"},
        )

    def test_system_status_is_ignored(self):
        self.assertIsNone(to_pet_event({
            "type": "system",
            "event": "live_status",
            "live": True,
        }))

    def test_unknown_message_is_ignored(self):
        self.assertIsNone(to_pet_event({"method": "WebcastUnknownMessage"}))


if __name__ == "__main__":
    unittest.main()
