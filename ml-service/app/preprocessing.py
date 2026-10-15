"""
Cleans bank transaction descriptions before they go into the model.

Used by both training (train.py) and the service (model.py), so the
model always sees text cleaned in exactly the same way.
"""

import re

PUNCTUATION = re.compile(r"[^\w\s]")
REFERENCE_CODE = re.compile(r"\b(?=\w*\d)(?=\w*[a-z])\w{8,}\b")
NUMBER = re.compile(r"\b\d+\b")
SPACES = re.compile(r"\s+")


def clean(text: str) -> str:
    text = text.lower()
    text = PUNCTUATION.sub(" ", text)
    text = REFERENCE_CODE.sub(" ", text)
    text = NUMBER.sub(" ", text)
    return SPACES.sub(" ", text).strip()