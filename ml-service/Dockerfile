FROM python:3.12-slim

WORKDIR /app

# Install dependencies first, so Docker can cache this step
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the code and the synthetic dataset
COPY app/ app/
COPY training/ training/
COPY data/transactions.csv data/transactions.csv

# Train the model inside the image
RUN python -m training.train

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]