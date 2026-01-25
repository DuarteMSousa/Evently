@echo off

kubectl apply -f ./infrastructure --recursive
kubectl apply -f ./ingress

kubectl apply -f ./services/cart

kubectl apply -f ./services/events

kubectl apply -f ./services/fileGeneration

timeout /t 60

kubectl apply -f ./services/notification

kubectl apply -f ./services/orders

kubectl apply -f ./services/organizations

timeout /t 60

kubectl apply -f ./services/payments

kubectl apply -f ./services/refunds

kubectl apply -f ./services/reviews

timeout /t 60

kubectl apply -f ./services/ticketManagement

kubectl apply -f ./services/tickets

timeout /t 60

kubectl apply -f ./services/users

kubectl apply -f ./services/venues


pause
